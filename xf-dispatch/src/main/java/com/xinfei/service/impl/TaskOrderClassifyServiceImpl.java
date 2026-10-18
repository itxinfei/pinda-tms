package com.xinfei.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xinfei.DTO.OrderClassifyDTO;
import com.xinfei.DTO.OrderClassifyGroupDTO;
import com.xinfei.DTO.OrderSearchDTO;
import com.xinfei.DTO.angency.AgencyScopeDto;
import com.xinfei.DTO.AreaDTO;
import com.xinfei.common.geo.GeoClient;
import com.xinfei.common.utils.EntCoordSyncJob;
import com.xinfei.common.utils.Result;
import com.xinfei.DTO.OrderDTO;
import com.xinfei.entity.OrderClassifyEntity;
import com.xinfei.entity.OrderClassifyOrderEntity;
import com.xinfei.enums.OrderStatus;
import com.xinfei.feign.OrderFeign;
import com.xinfei.feign.AreaFeign;
import com.xinfei.feign.agency.AgencyScopeFeign;
import com.xinfei.future.XfCompletableFuture;
import com.xinfei.service.IOrderClassifyOrderService;
import com.xinfei.service.IOrderClassifyService;
import com.xinfei.service.ITaskOrderClassifyService;
import com.xinfei.utils.IdConvert;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 订单分类实现类
 *
 * <p>性能/事务安全改造（长事务风险治理）：
 * 原 {@code execute} 带 {@code @Transactional}，在流内对每个订单<b>同步</b>调用百度地图 HTTP 与
 * 逐单 {@code areaFeign} 查区划，单机构百单级会把数据库事务持锁时间拉到分钟级，威胁库整体可用性。</p>
 *
 * <p>改造要点：
 * 1. 将"拉订单 + 拼地址/调地图（全部远程/外网调用）"从事务中移出，仅 {@link #saveRecord} 落库保持事务；
 * 2. {@code execute} 入口一次性扫描本批所有订单涉及的省/市/区 id，批量调用一次 {@code areaFeign}
 *    （{@code XfCompletableFuture.areaMapFuture}）拿到 {@code Map<Long, AreaDTO>} 并透传进拼地址逻辑，
 *    删除内部逐单 {@code .get()} 阻塞远程查；
 * 3. 给百度地理编码（地址→坐标）与反向地理编码（坐标→区划）加 10 分钟 TTL 的本地缓存，
 *    同一地址/坐标在缓存命中时不再发起外网 HTTP，彻底消除事务内对百度地图的外网阻塞。
 * 输入→输出语义保持等价。</p>
 */
@Service
@Slf4j
@SuppressWarnings({"rawtypes", "unchecked"})
public class TaskOrderClassifyServiceImpl implements ITaskOrderClassifyService {
    @Autowired
    private OrderFeign orderFeign;
    @Autowired
    private GeoClient geoClient;
    @Autowired
    private AgencyScopeFeign agencyScopeFeign;
    @Autowired
    private IOrderClassifyService orderClassifyService;
    @Autowired
    private IOrderClassifyOrderService orderClassifyOrderService;
    @Autowired
    private AreaFeign areaFeign;

    /**
     * 百度地理编码（地址→经纬度）本地缓存：10 分钟 TTL。
     * key=标准化地址字符串；value= "lng,lat"。仅缓存成功结果（null 不缓存，失败可重试）。
     * 同一地址在缓存命中时不再发起外网 HTTP，显著降低事务持锁时间。
     */
    private final Cache<String, String> coordinateCache = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(5000)
            .build();

    /**
     * 百度反向地理编码（经纬度→区划信息 Map）本地缓存：10 分钟 TTL。
     * key=坐标字符串 "lng,lat"；value= 百度返回的 addressComponent Map。仅缓存成功结果。
     */
    private final Cache<String, Map> reverseGeoCache = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(5000)
            .build();

    /**
     * 订单分类核心逻辑
     * @param orgId 机构id（网点或者转运中心的id）
     * @param jobId 定时任务id
     * @param logId 日志id
     * @return
     */
    @Override
    public List<OrderClassifyGroupDTO> execute(Long orgId, Long jobId, Long logId) {
        // ============ 以下为「非事务」阶段：含 Feign / 百度地图等远程调用 ============
        // 1) 拉取本机构下的新订单与中转订单（远程调用，不持库事务）
        List<OrderDTO> newOrderList = orderFeign.list(buildOrderSearch(orgId, OrderStatus.OUTLETS_WAREHOUSE.getCode()));
        List<OrderDTO> transferOrderList = orderFeign.list(buildOrderSearch(orgId, OrderStatus.IN_TRANSIT.getCode()));
        log.info("查询到新订单个数：{}，中转订单个数：{}", newOrderList.size(), transferOrderList.size());

        // 2) 扫描本批所有订单涉及的省/市/区 id，批量查询一次区划（仅一次 areaFeign 调用）
        Set<Long> areaIds = new HashSet<>();
        collectAreaIds(newOrderList, areaIds);
        collectAreaIds(transferOrderList, areaIds);
        Map<Long, AreaDTO> areaMap = Collections.emptyMap();
        if (!areaIds.isEmpty()) {
            areaMap = getAreaMap(areaIds);
        }

        // 3) 分类（远程调用主要在拼地址/调地图，已通过 areaMap 批量 + 地图缓存去重）
        List<OrderClassifyDTO> orderClassifyDTOS = new ArrayList<>();
        orderClassifyDTOS.addAll(classifyNewOrders(newOrderList, orgId, areaMap));
        orderClassifyDTOS.addAll(classifyTransferOrders(transferOrderList, orgId, areaMap));

        // 进行订单分类
        Map<String, List<OrderClassifyDTO>> orderClassifyDTOGroup = orderClassifyDTOS.stream().collect(Collectors.groupingBy(OrderClassifyDTO::groupBy));

        // 进行对象转换，将当前Map对象转为 List<OrderClassifyGroupDTO>类型
        List<OrderClassifyGroupDTO> orderClassifyGroupDTOS = new ArrayList<>();
        orderClassifyDTOGroup.forEach((key,value) -> {
            OrderClassifyGroupDTO.OrderClassifyGroupDTOBuilder groupBuilder = OrderClassifyGroupDTO.builder();
            groupBuilder.key(key);
            // 获取原始订单ID列表
            List<String> orderIds = value.stream().map((item) -> item.getOrder().getId()).collect(Collectors.toList());
            groupBuilder.orders(orderIds);
            OrderClassifyGroupDTO orderClassifyGroupDTO = groupBuilder.build();
            orderClassifyGroupDTOS.add(orderClassifyGroupDTO);
        });

        // ============ 以下为「事务」阶段：仅落库 ============
        // 将分类结果保存到数据库（独立事务，避免远程调用占用数据库长事务）
        saveRecord(orderClassifyGroupDTOS,jobId,logId);

        return orderClassifyGroupDTOS;
    }

    /**
     * 构造按机构/状态查询订单的搜索条件
     */
    private OrderSearchDTO buildOrderSearch(Long orgId, Integer status) {
        OrderSearchDTO orderSearchDTO = new OrderSearchDTO();
        orderSearchDTO.setStatus(status);
        // xf-oms 边界：currentAgencyId 为 String
        orderSearchDTO.setCurrentAgencyId(IdConvert.toStr(orgId));
        return orderSearchDTO;
    }

    /**
     * 一次性批量查询本批涉及的区划信息（替代原 buildFullAddress 内逐单 .get() 阻塞远程查）。
     */
    private Map<Long, AreaDTO> getAreaMap(Set<Long> areaIds) {
        try {
            CompletableFuture<Map<Long, AreaDTO>> future = XfCompletableFuture.areaMapFuture(areaFeign, null, areaIds);
            Map<Long, AreaDTO> map = future.get();
            return map == null ? Collections.emptyMap() : map;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw exceptionHappend("地址区划查询被中断");
        } catch (Exception e) {
            throw exceptionHappend("地址区划查询失败: " + e.getMessage());
        }
    }

    /**
     * 收集订单列表中发件/收件省、市、区 id（无效的静默跳过，不抛业务异常）。
     */
    private void collectAreaIds(List<OrderDTO> orders, Set<Long> areaIds) {
        if (CollectionUtils.isEmpty(orders)) {
            return;
        }
        for (OrderDTO order : orders) {
            addAreaId(areaIds, order.getSenderProvinceId());
            addAreaId(areaIds, order.getSenderCityId());
            addAreaId(areaIds, order.getSenderCountyId());
            addAreaId(areaIds, order.getReceiverProvinceId());
            addAreaId(areaIds, order.getReceiverCityId());
            addAreaId(areaIds, order.getReceiverCountyId());
        }
    }

    private void addAreaId(Set<Long> areaIds, String rawId) {
        Long id = tryParseAreaId(rawId);
        if (id != null) {
            areaIds.add(id);
        }
    }

    /**
     * 区划id 字符串转 Long：空白/非数字返回 null（收集阶段静默跳过，不抛异常）。
     */
    private Long tryParseAreaId(String rawId) {
        if (StringUtils.isBlank(rawId)) {
            return null;
        }
        try {
            return Long.valueOf(rawId.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 保存订单分类结果（独立事务，仅此处持有数据库事务，不再包裹任何远程/外网调用）
     * @param orderClassifyGroupDTOS
     * @param jobId
     * @param logId
     */
    @Transactional(rollbackFor = Exception.class)
    private void saveRecord(List<OrderClassifyGroupDTO> orderClassifyGroupDTOS, Long jobId, Long logId) {
        orderClassifyGroupDTOS.forEach(item -> {
            if (item.isNew()) {
                log.info("新订单 保存分组信息");
                OrderClassifyEntity entity = new OrderClassifyEntity();
                entity.setClassify(item.getKey());
                entity.setJobId(jobId);
                entity.setJobLogId(logId);
                if (!entity.getClassify().equals("ERROR")) {
                    entity.setStartOrgId(item.getStartOrgId());
                    entity.setEndOrgId(item.getEndOrgId());
                }
                entity.setTotal(item.getOrders().size());

                // 先保存主表，由 MyBatis-Plus 回填雪花ID，再构造关联明细
                orderClassifyService.save(entity);
                item.setId(entity.getId());

                List<OrderClassifyOrderEntity> orderClassifyOrders = item.getOrders().stream().map((orderId) -> {
                    OrderClassifyOrderEntity orderClassifyOrderEntity = new OrderClassifyOrderEntity();
                    // 订单ID为 xf-oms String 边界，落库 BIGINT 前转换
                    orderClassifyOrderEntity.setOrderId(IdConvert.toLong(orderId));
                    orderClassifyOrderEntity.setClassifyId(entity.getId());
                    return orderClassifyOrderEntity;
                }).collect(Collectors.toList());

                orderClassifyOrderService.saveBatch(orderClassifyOrders);
            } else {
                log.info("中转订单，查询分组信息");
                List<String> orderIds = new ArrayList<>(item.getOrders());
                log.info("当前分组的订单id：{}", orderIds);
                LambdaQueryWrapper<OrderClassifyOrderEntity> wrapper = new LambdaQueryWrapper<>();
                wrapper.in(OrderClassifyOrderEntity::getOrderId, IdConvert.toLongList(orderIds));
                List<OrderClassifyOrderEntity> orderClassifyOrders = orderClassifyOrderService.list(wrapper);
                // 不出意外只会查到一个订单分类id
                Set<Long> orderClassifyIds = orderClassifyOrders.stream().map(OrderClassifyOrderEntity::getClassifyId).collect(Collectors.toSet());
                log.info("查询订单分组id:{}", orderClassifyIds);
                if (CollectionUtils.isEmpty(orderClassifyIds)) {
                    log.error("中转订单异常:{}", orderIds);
                    // 修改点：原 continue 位于 forEach 的 lambda 内（非循环），改为 return 跳过当前元素
                    return;
                }
                item.setId(orderClassifyIds.iterator().next());
            }
        });
    }

    /**
     * 根据订单列表分类「新订单」（当前入库网点为起点）。
     * @param orderList 已拉取的订单列表（远程调用已在外层完成）
     * @param orgId     当前机构id
     * @param areaMap   本批批量查询出的区划映射
     */
    private List<OrderClassifyDTO> classifyNewOrders(List<OrderDTO> orderList, Long orgId, Map<Long, AreaDTO> areaMap) {
        if (CollectionUtils.isEmpty(orderList)) {
            return new ArrayList<>();
        }
        // 【P0事务隔离】同下：单订单分类失败仅跳过该订单，不中断整轮调度。
        List<OrderClassifyDTO> orderClassifyDTOList = orderList.stream().map((item) -> {
            try {
                OrderClassifyDTO.OrderClassifyDTOBuilder orderBuilder = OrderClassifyDTO.builder();
                orderBuilder.order(item);//原始订单
                orderBuilder.startOrgId(orgId);//起始机构（当前入库网点）
                orderBuilder.endOrgId(IdConvert.toLong(getEndAgencyId(item, areaMap)));//目的地机构（目的地网点）
                orderBuilder.orderType(item.getOrderType());
                orderBuilder.currentOrgId(orgId);
                return orderBuilder.build();
            } catch (Exception e) {
                log.error("[classifyNewOrders] 订单[{}]分类失败，跳过该订单（不影响本机构其余订单调度）: {}",
                        item != null ? item.getId() : "null", e.getMessage());
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());

        return orderClassifyDTOList;
    }

    /**
     * 根据订单列表分类「中转订单」（运输中）。
     * @param orderList 已拉取的订单列表（远程调用已在外层完成）
     * @param orgId     当前机构id
     * @param areaMap   本批批量查询出的区划映射
     */
    private List<OrderClassifyDTO> classifyTransferOrders(List<OrderDTO> orderList, Long orgId, Map<Long, AreaDTO> areaMap) {
        if (CollectionUtils.isEmpty(orderList)) {
            return new ArrayList<>();
        }
        // 【P0事务隔离】单订单分类失败（地址异常/区域不一致/网点缺失/地图异常等）不应中断
        // 整个机构的调度轮次：此处对每条订单单独 try-catch，失败订单仅记录日志并跳过（filter 掉），
        // 其余订单继续参与分类与调度，避免"一单出错、整轮回滚、全部漏调度"。
        List<OrderClassifyDTO> orderClassifyDTOList = orderList.stream().map((item) -> {
            try {
                OrderClassifyDTO.OrderClassifyDTOBuilder orderBuilder = OrderClassifyDTO.builder();
                orderBuilder.order(item);//原始订单
                orderBuilder.startOrgId(IdConvert.toLong(getStartAgencyId(item, areaMap)));//起始机构
                orderBuilder.endOrgId(IdConvert.toLong(getEndAgencyId(item, areaMap)));//目的地机构（目的地网点）
                orderBuilder.orderType(item.getOrderType());
                orderBuilder.currentOrgId(orgId);
                return orderBuilder.build();
            } catch (Exception e) {
                log.error("[classifyTransferOrders] 订单[{}]分类失败，跳过该订单（不影响本机构其余订单调度）: {}",
                        item != null ? item.getId() : "null", e.getMessage());
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());

        return orderClassifyDTOList;
    }

    /**
     * 根据订单获得起始机构id
     * @param order   订单
     * @param areaMap 本批批量区划映射
     * @return
     */
    private String getStartAgencyId(OrderDTO order, Map<Long, AreaDTO> areaMap) {
        // 根据当前订单获取发件人地址详细信息，包含省市区
        String address = senderFullAddress(order, areaMap);
        if(StringUtils.isBlank(address)){
            throw exceptionHappend("发件人地址不能为空");
        }

        // 调用百度地图工具类，根据地址获取对应的经纬度坐标（走本地缓存，命中则不发外网）
        String location = getCoordinateCached(address);
        if(StringUtils.isBlank(location)){
            // 百度地图不可用（未配置AK/外网不通）：按发件区县确定性匹配覆盖网点，
            // 不依赖外网、结果可重复，保证内网调度链路不被地图服务中断
            String fallbackAgencyId = resolveAgencyByCountyId(order.getSenderCountyId());
            if(StringUtils.isNotBlank(fallbackAgencyId)){
                log.info("百度地图不可用，按发件区县[{}]确定性匹配起始网点[{}]", order.getSenderCountyId(), fallbackAgencyId);
                return fallbackAgencyId;
            }
            throw exceptionHappend("发件人地址不正确");
        }

        log.info("根据地址{}获取对应的坐标值{}",address,location);

        // 根据经纬度坐标获取对应的区县检查通过百度地图获取的区域是否和下单时选择的区域一致
        Map map = getLocationByPositionCached(location);// 根据经纬度获取对应的区域信息（从百度官方获取，走缓存）
        if(ObjectUtils.isEmpty(map)){
            throw exceptionHappend("根据经纬度获取区域信息为空");
        }

        String adcode = (String) map.get("adcode");

        // 根据adcode（区域编码）查询我们系统中的区域信息
        AreaDTO area = areaFeign.getByCode(adcode + "000000");
        if(area == null){
            throw exceptionHappend("没有查询到区域数据");
        }

        // order 侧的区域 id 是 String（列 varchar(20)），area.getId() 是 Long，
        // 直接 equals 恒 false，会让每一单都被判成"区域不一致"而无法进分单
        if (!StringUtils.equals(order.getSenderCountyId(), String.valueOf(area.getId()))) {
            throw exceptionHappend("发件地址区域id和根据坐标计算出的区域不一致");
        }

        // 查询当前区县下的所有网点
        List<AgencyScopeDto> agencyScope = agencyScopeFeign.findAllAgencyScope(area.getId().toString(), null, null, null);
        if(agencyScope == null || agencyScope.size() == 0){
            throw exceptionHappend("无法获取收件人所在网点信息");
        }

        // 计算当前区域下所有网点距离收件人地址最近的网点
        Result result = caculate(agencyScope,location);

        return (String) result.get("agencyId");
    }

    /**
     * 获得发件人详细地址信息
     * @param order   订单
     * @param areaMap 本批批量区划映射
     * @return
     */
    private String senderFullAddress(OrderDTO order, Map<Long, AreaDTO> areaMap) {
        return buildFullAddress(areaMap, "发件人", order.getSenderProvinceId(), order.getSenderCityId(),
                order.getSenderCountyId(), order.getSenderAddress());
    }

    /**
     * 拼装「省+市+区+详细地址」全地址（发件/收件共用）。
     *
     * <p>健壮性约束（区划数据缺失必须给出业务原因，不能抛 NPE）：区划数据来自调用方批量透传的
     * {@code areaMap}（已在 execute 入口一次性查询），不再在方法内逐单发起 {@code areaFeign} 远程阻塞调用。</p>
     *
     * @param areaMap  本批批量区划映射（id -> AreaDTO）
     * @param roleLabel 角色标签（发件人/收件人），用于异常文案
     * @param provinceId 省id
     * @param cityId     市id
     * @param countyId   区县id
     * @param detail     详细地址
     * @return 拼接后的完整地址
     */
    private String buildFullAddress(Map<Long, AreaDTO> areaMap, String roleLabel, String provinceId, String cityId,
                                    String countyId, String detail) {
        Long province = parseAreaId(provinceId, roleLabel + "省");
        Long city = parseAreaId(cityId, roleLabel + "市");
        Long county = parseAreaId(countyId, roleLabel + "区");

        if (areaMap == null || areaMap.isEmpty()) {
            throw exceptionHappend(roleLabel + "地址区划数据缺失（区划服务无返回）");
        }

        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(requireAreaName(areaMap, province, roleLabel + "省"))
                .append(requireAreaName(areaMap, city, roleLabel + "市"))
                .append(requireAreaName(areaMap, county, roleLabel + "区"))
                .append(StringUtils.isBlank(detail) ? "" : detail.trim());

        return stringBuffer.toString();
    }

    /**
     * 区划id 字符串转 Long：空白/非数字一律转成明确业务异常（避免 Long.valueOf 抛 NumberFormatException）
     */
    private Long parseAreaId(String rawId, String label) {
        if (StringUtils.isBlank(rawId)) {
            throw exceptionHappend(label + "区划id为空");
        }
        try {
            return Long.valueOf(rawId.trim());
        } catch (NumberFormatException e) {
            throw exceptionHappend(label + "区划id非法: " + rawId);
        }
    }

    /**
     * 取区划名称，缺失时给出带层级与id 的业务异常（替代原 NPE）
     */
    private String requireAreaName(Map<Long, AreaDTO> areaMap, Long areaId, String label) {
        AreaDTO area = areaMap.get(areaId);
        if (area == null || StringUtils.isBlank(area.getName())) {
            throw exceptionHappend(label + "区划数据缺失: id=" + areaId);
        }
        return area.getName();
    }

    /**
     * 获取目的地网点id
     * @param order   订单
     * @param areaMap 本批批量区划映射
     * @return
     */
    private String getEndAgencyId(OrderDTO order, Map<Long, AreaDTO> areaMap) {
        // 根据当前订单获取收件人地址详细信息，包含省市区，例如：北京市昌平区建材城西路金燕龙办公楼
        String address = receiverFullAddress(order, areaMap);
        if(StringUtils.isBlank(address)){
            throw exceptionHappend("收件人地址不能为空");
        }

        // 调用百度地图工具类，根据地址获取对应的经纬度坐标（走本地缓存，命中则不发外网）
        String location = getCoordinateCached(address);
        if(StringUtils.isBlank(location)){
            // 百度地图不可用（未配置AK/外网不通）：按收件区县确定性匹配覆盖网点，
            // 不依赖外网、结果可重复，保证内网调度链路不被地图服务中断
            String fallbackAgencyId = resolveAgencyByCountyId(order.getReceiverCountyId());
            if(StringUtils.isNotBlank(fallbackAgencyId)){
                log.info("百度地图不可用，按收件区县[{}]确定性匹配目的网点[{}]", order.getReceiverCountyId(), fallbackAgencyId);
                return fallbackAgencyId;
            }
            throw exceptionHappend("收件人地址不正确");
        }

        log.info("根据地址{}获取对应的坐标值{}",address,location);

        // 根据经纬度获取对应的区县检查通过百度地图获取的区域是否和下单时选择的区域一致
        Map map = getLocationByPositionCached(location);// 根据经纬度获取对应的区域信息（从百度官方获取，走缓存）
        if(ObjectUtils.isEmpty(map)){
            throw exceptionHappend("根据经纬度获取区域信息为空");
        }

        String adcode = (String) map.get("adcode");

        // 根据adcode（区域编码）查询我们系统中的区域信息
        AreaDTO area = areaFeign.getByCode(adcode + "000000");
        if(area == null){
            throw exceptionHappend("没有查询到区域数据");
        }

        // 同上：Area.id 是 Long、order 侧县 id 是 varchar，直接 equals 恒 false
        if(!StringUtils.equals(order.getReceiverCountyId(), String.valueOf(area.getId()))){
            throw exceptionHappend("收货地址区域id和根据坐标计算出的区域不一致");
        }

        // 查询当前区县下的所有网点
        List<AgencyScopeDto> agencyScope = agencyScopeFeign.findAllAgencyScope(area.getId().toString(), null, null, null);
        if(agencyScope == null || agencyScope.size() == 0){
            throw exceptionHappend("无法获取收件人所在网点信息");
        }

        // 计算当前区域下所有网点距离收件人地址最近的网点
        Result result = caculate(agencyScope,location);

        return (String) result.get("agencyId");
    }

    /**
     * 从给定网点中查找覆盖指定点的网点
     * @param agencyScopes
     * @param location
     * @return
     */
    private Result caculate(List<AgencyScopeDto> agencyScopes,String location) {
        try {
            for (AgencyScopeDto agencyScopeDto : agencyScopes) {
                List<List<Map>> mutiPoints = agencyScopeDto.getMutiPoints();
                for (List<Map> list : mutiPoints) {
                    String[] originArray = location.split(",");
                    // 判断某个点是否在指定区域范围内
                    boolean flag = EntCoordSyncJob.isInScope(list, Double.parseDouble(originArray[0]), Double.parseDouble(originArray[1]));
                    if (flag) {
                        return Result.ok().put("agencyId", agencyScopeDto.getAgencyId());
                    }
                }
            }
            return Result.error(5000, "获取网点失败");
        } catch (Exception e) {
            log.error("获取网点失败", e);
            return Result.error(5000, "获取网点失败");
        }
    }

    /**
     * 抛出业务异常，中断当前流程
     * @param msg 错误信息
     */
    private RuntimeException exceptionHappend(String msg){
        return new RuntimeException(msg);
    }

    /**
     * 确定性兜底：按区县id查询覆盖该区域的网点（不依赖百度地图）。
     * 取该区县下配置的第一个网点，供百度地图不可用时使用。
     * @param countyId 区县id
     * @return 网点id，无配置时返回 null
     */
    private String resolveAgencyByCountyId(String countyId){
        if(StringUtils.isBlank(countyId)){
            return null;
        }
        List<AgencyScopeDto> agencyScopes = agencyScopeFeign.findAllAgencyScope(countyId, null, null, null);
        if(agencyScopes == null || agencyScopes.isEmpty()){
            return null;
        }
        return agencyScopes.get(0).getAgencyId();
    }

    /**
     * 百度地理编码（地址→经纬度），带 10 分钟本地缓存。
     * 仅缓存成功结果（非 null）；失败（百度不可用/超时）不缓存，便于后续重试恢复。
     * @param address 标准化后的地址字符串
     * @return "lng,lat" 或 null
     */
    private String getCoordinateCached(String address) {
        if (StringUtils.isBlank(address)) {
            return null;
        }
        String cached = coordinateCache.getIfPresent(address);
        if (cached != null) {
            return cached;
        }
        String coordinate = geoClient.geocode(address);
        if (coordinate != null) {
            coordinateCache.put(address, coordinate);
        }
        return coordinate;
    }

    /**
     * 百度反向地理编码（经纬度→区划信息），带 10 分钟本地缓存。
     * 仅缓存成功结果（非 null）；失败不缓存，便于后续重试恢复。
     * @param location 坐标字符串 "lng,lat"
     * @return 百度返回的 addressComponent Map 或 null
     */
    private Map getLocationByPositionCached(String location) {
        if (StringUtils.isBlank(location)) {
            return null;
        }
        Map cached = reverseGeoCache.getIfPresent(location);
        if (cached != null) {
            return cached;
        }
        Map result = geoClient.reverseGeocode(location);
        if (result != null) {
            reverseGeoCache.put(location, result);
        }
        return result;
    }

    /**
     * 获取坐标值
     * @param map
     * @return
     */
    private String getPoint(Map map){
        String lng = map.get("lng").toString();
        String lat = map.get("lat").toString();
        return lng + "," + lat;
    }

    /**
     * 根据订单获取对应的完整收件人地址信息
     * @param order   订单
     * @param areaMap 本批批量区划映射
     * @return
     */
    private String receiverFullAddress(OrderDTO order, Map<Long, AreaDTO> areaMap) {
        return buildFullAddress(areaMap, "收件人", order.getReceiverProvinceId(), order.getReceiverCityId(),
                order.getReceiverCountyId(), order.getReceiverAddress());
    }


}
