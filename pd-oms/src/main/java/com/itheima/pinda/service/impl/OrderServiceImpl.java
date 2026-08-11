package com.itheima.pinda.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.pinda.DTO.OrderDTO;
import com.itheima.pinda.DTO.OrderSearchDTO;
import com.itheima.pinda.common.utils.BaiduMapUtils;
import com.itheima.pinda.common.utils.CustomIdGenerator;
import com.itheima.pinda.entity.Order;
import com.itheima.pinda.entity.fact.AddressCheckResult;
import com.itheima.pinda.entity.fact.AddressRule;
import com.itheima.pinda.enums.OrderPaymentStatus;
import com.itheima.pinda.enums.OrderPickupType;
import com.itheima.pinda.enums.OrderStatus;
import com.itheima.pinda.mapper.OrderMapper;
import com.itheima.pinda.service.IOrderService;
import org.apache.commons.lang.StringUtils;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 订单服务实现类
 */
@Slf4j
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements IOrderService {

    /**
     * 订单状态流转图（Key: 当前状态, Value: 允许的下一个状态集合）
     * 与 pd-work 的 StateTransitionValidator 保持一致，并依据实际业务路径补充：
     *  - 网点自寄(23002) → 网点入库(23003)：自寄订单交件直接入库
     *  - 网点出库(23006) → 派送中(23008)：快递员接件直接进入派送（跳过待派送 23007）
     */
    private static final Map<Integer, Set<Integer>> ORDER_STATUS_TRANSITIONS = new HashMap<>();

    static {
        // 待取件 → 已取件 / 已取消
        ORDER_STATUS_TRANSITIONS.put(OrderStatus.PENDING.getCode(), new HashSet<>(Arrays.asList(
            OrderStatus.PICKED_UP.getCode(), OrderStatus.CANCELLED.getCode())));
        // 已取件 → 网点入库 / 已取消
        ORDER_STATUS_TRANSITIONS.put(OrderStatus.PICKED_UP.getCode(), new HashSet<>(Arrays.asList(
            OrderStatus.OUTLETS_WAREHOUSE.getCode(), OrderStatus.CANCELLED.getCode())));
        // 网点自寄 → 网点入库 / 已取消
        ORDER_STATUS_TRANSITIONS.put(OrderStatus.OUTLETS_SINCE_SENT.getCode(), new HashSet<>(Arrays.asList(
            OrderStatus.OUTLETS_WAREHOUSE.getCode(), OrderStatus.CANCELLED.getCode())));
        // 网点入库 → 待装车 / 已取消
        ORDER_STATUS_TRANSITIONS.put(OrderStatus.OUTLETS_WAREHOUSE.getCode(), new HashSet<>(Arrays.asList(
            OrderStatus.FOR_LOADING.getCode(), OrderStatus.CANCELLED.getCode())));
        // 待装车 → 运输中 / 已取消
        ORDER_STATUS_TRANSITIONS.put(OrderStatus.FOR_LOADING.getCode(), new HashSet<>(Arrays.asList(
            OrderStatus.IN_TRANSIT.getCode(), OrderStatus.CANCELLED.getCode())));
        // 运输中 → 网点出库 / 已取消
        ORDER_STATUS_TRANSITIONS.put(OrderStatus.IN_TRANSIT.getCode(), new HashSet<>(Arrays.asList(
            OrderStatus.OUTLETS_EX_WAREHOUSE.getCode(), OrderStatus.CANCELLED.getCode())));
        // 网点出库 → 待派送 / 派送中 / 已取消
        ORDER_STATUS_TRANSITIONS.put(OrderStatus.OUTLETS_EX_WAREHOUSE.getCode(), new HashSet<>(Arrays.asList(
            OrderStatus.TO_BE_DISPATCHED.getCode(), OrderStatus.DISPATCHING.getCode(), OrderStatus.CANCELLED.getCode())));
        // 待派送 → 派送中 / 已取消
        ORDER_STATUS_TRANSITIONS.put(OrderStatus.TO_BE_DISPATCHED.getCode(), new HashSet<>(Arrays.asList(
            OrderStatus.DISPATCHING.getCode(), OrderStatus.CANCELLED.getCode())));
        // 派送中 → 已签收 / 拒收 / 已取消
        ORDER_STATUS_TRANSITIONS.put(OrderStatus.DISPATCHING.getCode(), new HashSet<>(Arrays.asList(
            OrderStatus.RECEIVED.getCode(), OrderStatus.REJECTION.getCode(), OrderStatus.CANCELLED.getCode())));
        // 已签收、拒收、已取消 → 终态
        ORDER_STATUS_TRANSITIONS.put(OrderStatus.RECEIVED.getCode(), Collections.emptySet());
        ORDER_STATUS_TRANSITIONS.put(OrderStatus.REJECTION.getCode(), Collections.emptySet());
        ORDER_STATUS_TRANSITIONS.put(OrderStatus.CANCELLED.getCode(), Collections.emptySet());
    }

    /**
     * 修改订单（重写以接入状态流转校验）
     *
     * <p>所有订单状态变更（Feign 调用、管理端、司机/快递员端）最终都经过 {@code updateById}，
     * 因此在此统一校验状态流转合法性，仅当状态真正发生变化时才校验，
     * 避免普通字段更新被误拦截。</p>
     *
     * @param order 待更新的订单（需包含 id）
     * @return 是否更新成功；状态流转非法时返回 false 并记录错误日志
     */
    @Override
    public boolean updateById(Order order) {
        if (order == null || StringUtils.isBlank(order.getId())) {
            log.warn("订单更新失败：订单ID为空");
            return false;
        }

        // 读取当前持久化状态，用于校验流转合法性
        Order existing = getById(order.getId());
        if (existing == null) {
            log.warn("订单[{}]不存在，无法更新", order.getId());
            return false;
        }

        // 状态维度校验（仅在 status 发生变化时校验）
        Integer newStatus = order.getStatus();
        if (newStatus != null && !newStatus.equals(existing.getStatus())) {
            Set<Integer> allowedTransitions = ORDER_STATUS_TRANSITIONS.get(existing.getStatus());
            if (allowedTransitions == null || !allowedTransitions.contains(newStatus)) {
                log.error("订单[{}]状态流转非法：当前状态[{}]不能流转到[{}]",
                    order.getId(), existing.getStatus(), newStatus);
                return false;
            }
        }

        return super.updateById(order);
    }

    @Autowired
    private CustomIdGenerator idGenerator;
    @Autowired
    private ReloadDroolsRulesService reloadDroolsRulesService;

    @Override
    public Order saveOrder(Order order) {
        order.setId(idGenerator.nextId(order) + "");
        order.setCreateTime(LocalDateTime.now());
        order.setPaymentStatus(OrderPaymentStatus.UNPAID.getStatus());
        if (OrderPickupType.NO_PICKUP.getCode() == order.getPickupType()) {
            order.setStatus(OrderStatus.OUTLETS_SINCE_SENT.getCode());
        } else {
            order.setStatus(OrderStatus.PENDING.getCode());
        }
        save(order);
        return order;
    }

    @Override
    public IPage<Order> findByPage(Integer page, Integer pageSize, Order order) {
        Page<Order> iPage = new Page(page, pageSize);
        LambdaQueryWrapper<Order> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotEmpty(order.getId())) {
            lambdaQueryWrapper.eq(Order::getId, order.getId());
        }
        if (order.getStatus() != null) {
            lambdaQueryWrapper.eq(Order::getStatus, order.getStatus());
        }
        if (order.getPaymentStatus() != null) {
            lambdaQueryWrapper.eq(Order::getPaymentStatus, order.getPaymentStatus());
        }
        //发件人信息
        if (StringUtils.isNotEmpty(order.getSenderName())) {
            lambdaQueryWrapper.like(Order::getSenderName, order.getSenderName());
        }
        if (StringUtils.isNotEmpty(order.getSenderPhone())) {
            lambdaQueryWrapper.like(Order::getSenderPhone, order.getSenderPhone());
        }
        if (StringUtils.isNotEmpty(order.getSenderProvinceId())) {
            lambdaQueryWrapper.eq(Order::getSenderProvinceId, order.getSenderProvinceId());
        }
        if (StringUtils.isNotEmpty(order.getSenderCityId())) {
            lambdaQueryWrapper.eq(Order::getSenderCityId, order.getSenderCityId());
        }
        if (StringUtils.isNotEmpty(order.getSenderCountyId())) {
            lambdaQueryWrapper.eq(Order::getSenderCountyId, order.getSenderCountyId());
        }
        //收件人信息
        if (StringUtils.isNotEmpty(order.getReceiverName())) {
            lambdaQueryWrapper.like(Order::getReceiverName, order.getReceiverName());
        }
        if (StringUtils.isNotEmpty(order.getReceiverPhone())) {
            lambdaQueryWrapper.like(Order::getReceiverPhone, order.getReceiverPhone());
        }
        if (StringUtils.isNotEmpty(order.getReceiverProvinceId())) {
            lambdaQueryWrapper.eq(Order::getReceiverProvinceId, order.getReceiverProvinceId());
        }
        if (StringUtils.isNotEmpty(order.getReceiverCityId())) {
            lambdaQueryWrapper.eq(Order::getReceiverCityId, order.getReceiverCityId());
        }
        if (StringUtils.isNotEmpty(order.getReceiverCountyId())) {
            lambdaQueryWrapper.eq(Order::getReceiverCountyId, order.getReceiverCountyId());
        }
        lambdaQueryWrapper.orderBy(true, false, Order::getId);
        return page(iPage, lambdaQueryWrapper);
    }

    @Override
    public List<Order> findAll(List<String> ids) {
        LambdaQueryWrapper<Order> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        if (ids != null && ids.size() > 0) {
            lambdaQueryWrapper.in(Order::getId, ids);
        }
        lambdaQueryWrapper.orderBy(true, false, Order::getId);
        return list(lambdaQueryWrapper);
    }

    @Override
    public IPage<Order> pageLikeForCustomer(OrderSearchDTO orderSearchDTO) {

        Integer page = orderSearchDTO.getPage();
        Integer pageSize = orderSearchDTO.getPageSize();

        IPage<Order> ipage = new Page<>(page, pageSize);

        LambdaQueryWrapper<Order> orderQueryWrapper = new LambdaQueryWrapper<>();
        orderQueryWrapper.eq(StringUtils.isNotEmpty(orderSearchDTO.getId()), Order::getId, orderSearchDTO.getId());
        orderQueryWrapper.like(StringUtils.isNotEmpty(orderSearchDTO.getKeyword()), Order::getId, orderSearchDTO.getKeyword());
        orderQueryWrapper.eq(StringUtils.isNotEmpty(orderSearchDTO.getMemberId()), Order::getMemberId, orderSearchDTO.getMemberId());
        orderQueryWrapper.eq(StringUtils.isNotEmpty(orderSearchDTO.getReceiverPhone()), Order::getReceiverPhone, orderSearchDTO.getReceiverPhone());
        orderQueryWrapper.orderByDesc(Order::getCreateTime);
        return page(ipage, orderQueryWrapper);
    }

    //@Autowired
    //private KieContainer kieContainer;

    /**
     * 计算订单价格
     * @param orderDTO
     * @return
     */
    public Map calculateAmount(OrderDTO orderDTO) {
        //计算订单距离
        orderDTO = this.getDistance(orderDTO);

        if("sender error msg".equals(orderDTO.getSenderAddress()) || "receiver error msg".equals(orderDTO.getReceiverAddress())){
            //地址解析失败，直接返回
            Map map = new HashMap();
            map.put("amount","0");
            map.put("errorMsg","无法计算订单距离和订单价格，请输入真实地址");
            map.put("orderDto",orderDTO);
            return map;
        }

        if (orderDTO.getOrderCargoDto() == null || orderDTO.getDistance() == null) {
            log.warn("[订单价格计算] 参数不完整: orderCargoDto={}, distance={}", orderDTO.getOrderCargoDto(), orderDTO.getDistance());
            return null;
        }

        KieContainer container = reloadDroolsRulesService.getKieContainer();
        if (container == null) {
            log.error("[订单价格计算] Drools规则引擎未初始化，无法计算订单价格");
            return null;
        }
        //修改点：修复提前 return i 导致结果组装逻辑不可达、且返回值类型错误（应为 Map）的问题
        AddressCheckResult addressCheckResult = new AddressCheckResult();
        KieSession session = null;
        try {
            session = container.newKieSession();
            //设置Fact对象
            AddressRule addressRule = new AddressRule();
            if (orderDTO.getOrderCargoDto() != null && orderDTO.getOrderCargoDto().getTotalWeight() != null) {
                addressRule.setTotalWeight(orderDTO.getOrderCargoDto().getTotalWeight().doubleValue());
            }
            if (orderDTO.getDistance() != null) {
                addressRule.setDistance(orderDTO.getDistance().doubleValue());
            }

            //将对象加入到工作内存
            session.insert(addressRule);

            session.insert(addressCheckResult);

            int i = session.fireAllRules();
            log.info("触发了{}条规则", i);
        } finally {
            if (session != null) {
                session.destroy();
            }
        }

        if(addressCheckResult.isPostCodeResult()){
            log.info("规则匹配成功,订单价格为：{}", addressCheckResult.getResult());
            orderDTO.setAmount(new BigDecimal(addressCheckResult.getResult()));

            Map map = new HashMap();
            map.put("orderDto",orderDTO);
            map.put("amount",addressCheckResult.getResult());

            return map;
        }

        return null;
    }

    /**
     * 调用百度地图服务接口，根据寄件人地址和收件人地址计算订单距离
     * @param orderDTO
     * @return
     */
    public OrderDTO getDistance(OrderDTO orderDTO){
        //调用百度地图服务接口获取寄件人地址对应的坐标经纬度
        String begin = BaiduMapUtils.getCoordinate(orderDTO.getSenderAddress());
        if(begin == null){
            orderDTO.setSenderAddress("sender error msg");
            return orderDTO;
        }

        //调用百度地图服务接口获取收件人地址对应的坐标经纬度
        String end = BaiduMapUtils.getCoordinate(orderDTO.getReceiverAddress());
        if(end == null){
            orderDTO.setReceiverAddress("receiver error msg");
            return orderDTO;
        }

        Double distance = BaiduMapUtils.getDistance(begin, end);
        DecimalFormat decimalFormat = new DecimalFormat("#.##");
        String distanceStr = decimalFormat.format(distance/1000);

        orderDTO.setDistance(new BigDecimal(distanceStr));

        return orderDTO;
    }
}
