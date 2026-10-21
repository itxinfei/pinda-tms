package com.xinfei.service.impl;

import com.xinfei.DTO.OrderClassifyGroupDTO;
import com.xinfei.DTO.OrderLineSimpleDTO;
import com.xinfei.DTO.OrderLineTripsTruckDriverDTO;
import com.xinfei.DTO.TripsTruckDriverDTO;
import com.xinfei.DTO.transportline.TransportTripsDto;
import com.xinfei.DTO.transportline.TransportTripsTruckDriverDto;
import com.xinfei.DTO.truck.TruckDto;
import com.xinfei.DTO.UserDTO;
import com.xinfei.common.config.dynamic.DynamicConfigService;
import com.xinfei.common.utils.DateUtils;
import com.xinfei.entity.CacheLineDetailEntity;
import com.xinfei.entity.OrderClassifyAttachEntity;
import com.xinfei.feign.NettyTraceFeign;
import com.xinfei.feign.transportline.TransportTripsFeign;
import com.xinfei.feign.truck.TruckFeign;
import com.xinfei.feign.UserFeign;
import com.xinfei.service.IOrderClassifyAttachService;
import com.xinfei.service.ITaskTripsSchedulingService;
import com.xinfei.utils.IdConvert;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 查找车次 车辆 司机
 *
 * @author wangsongyan
 */
@Service
@Slf4j
public class TaskTripsSchedulingServiceImpl implements ITaskTripsSchedulingService {

    @Autowired
    private TransportTripsFeign transportTripsFeign;

    @Autowired
    private TruckFeign truckFeign;

    @Autowired
    private UserFeign userFeign;

    @Autowired
    private IOrderClassifyAttachService orderClassifyAttachService;
    @Autowired
    private NettyTraceFeign nettyTraceFeign;
    @Autowired
    private DynamicConfigService dynamicConfigService;

    @Override
    public List<OrderLineTripsTruckDriverDTO> execute(List<OrderLineSimpleDTO> orderLineSimpleDTOS, Long businessId, Long jobId, Long logId, Long agencyId) {
        log.info("车次 车辆 司机 规划 {}", orderLineSimpleDTOS);
        List<OrderLineTripsTruckDriverDTO> orderLineTripsTruckDriverDTOS = new ArrayList<>();
        orderLineSimpleDTOS.forEach(orderLineSimpleDTO -> {
            CacheLineDetailEntity cacheLineDetailEntity = orderLineSimpleDTO.getCacheLineDetailEntity();


            // 根据线路获取车次（xfwl-base 边界：transportLineId 为 String）
            List<TransportTripsDto> transportTripsDtos = transportTripsFeign.findAll(IdConvert.toStr(cacheLineDetailEntity.getTransportLineId()), null);
            if (CollectionUtils.isEmpty(transportTripsDtos)) {
                return;
            }
            log.info("根据线路获取车次:{}", transportTripsDtos);

            // 获取车次关联信息
            List<TransportTripsTruckDriverDto> transportTripsTruckDriverDtos = transportTripsFeign.findAllTruckDriverTransportTrips(transportTripsDtos.get(0).getId(), null, null);
            if (CollectionUtils.isEmpty(transportTripsTruckDriverDtos)) {
                return;
            }
            log.info("获取车次关联信息:{}", transportTripsTruckDriverDtos);

            // 可用车次列表 遍历集合  一个车次会有多个车和司机，获取状态正常的车次
            List<TripsTruckDriverDTO> tripsTruckDriverDTOS = getTripsTruckDriver(transportTripsTruckDriverDtos);
            if (CollectionUtils.isEmpty(tripsTruckDriverDTOS)) {
                return;
            }
            log.info("可用车次列表:{}", tripsTruckDriverDTOS);

            // 便利全部可用车次 根据时间匹配车次 （根据车次发车时间（小时：分钟）与当前时间比较，最近发车的车次作为最优方案）
            TripsTruckDriverDTO tripsTruckDriverDTO = getBestTripsTruckDriverDTO(tripsTruckDriverDTOS);
            if (tripsTruckDriverDTO == null) {
                return;
            }
            log.info("最优车次:{}", tripsTruckDriverDTO);

            //记录 订单分类id 关联车次 车辆 司机（使用最优车次）
            relation(tripsTruckDriverDTO, orderLineSimpleDTO.getOrderClassifyGroupDTOS());
            log.info("记录车次信息: tripsId={}, truckId={}, driverId={}",
                tripsTruckDriverDTO.getTripsId(), tripsTruckDriverDTO.getTruckId(), tripsTruckDriverDTO.getDriverId());

            orderLineTripsTruckDriverDTOS.add(new OrderLineTripsTruckDriverDTO(tripsTruckDriverDTO, IdConvert.toStr(cacheLineDetailEntity.getTransportLineId()), orderLineSimpleDTO.getOrderClassifyGroupDTOS()));


        });
        return orderLineTripsTruckDriverDTOS;
    }

    /**
     * 记录 订单分类id 关联车次 车辆 司机
     *
     * @param tripsTruckDriverDTO
     * @param orderClassifyGroupDTOS
     */
    private void relation(TripsTruckDriverDTO tripsTruckDriverDTO, List<OrderClassifyGroupDTO> orderClassifyGroupDTOS) {
        orderClassifyGroupDTOS.forEach(orderClassifyGroupDTO -> {
            OrderClassifyAttachEntity orderClassifyAttachEntity = new OrderClassifyAttachEntity();
            orderClassifyAttachEntity.setClassifyId(orderClassifyGroupDTO.getId());
            // 车次/车辆ID为 xfwl-base String 边界，落库 BIGINT 前转换；司机ID内部即 Long
            orderClassifyAttachEntity.setTripsId(IdConvert.toLong(tripsTruckDriverDTO.getTripsId()));
            orderClassifyAttachEntity.setTruckId(IdConvert.toLong(tripsTruckDriverDTO.getTruckId()));
            orderClassifyAttachEntity.setDriverId(tripsTruckDriverDTO.getDriverId());
            orderClassifyAttachService.save(orderClassifyAttachEntity);
        });

    }

    /**
     * 获取最优方案
     *
     * @param tripsTruckDriverDTOS
     * @return
     */
    private TripsTruckDriverDTO getBestTripsTruckDriverDTO(List<TripsTruckDriverDTO> tripsTruckDriverDTOS) {
        List<String> tripsIds = tripsTruckDriverDTOS.stream().map(item -> item.getTripsId()).collect(Collectors.toList());

        if (CollectionUtils.isEmpty(tripsIds)) {
            log.warn("关联车次信息未找到！");
            return null;
        }

        List<TransportTripsDto> transportTripsDtos = transportTripsFeign.findAll(null, tripsIds);
        // 仅远程调用失败(fallback 返回 null)时按无数据降级，避免 NPE；
        // 空列表(非 null)保持原有行为：走下方"未找到最优方案"兜底调度
        if (transportTripsDtos == null) {
            return null;
        }

        LocalDateTime nowDate = LocalDateTime.now();

        TripsTruckDriverDTO tripsTruckDriverDTO = null;
        // 当天 发车时间最近的优先
        log.info("开始查找最优方案 共：{}条", transportTripsDtos.size());
        for (TransportTripsDto item : transportTripsDtos.stream().sorted(Comparator.comparing(TransportTripsDto::getDepartureTime)).collect(Collectors.toList())) {
            String[] departures = item.getDepartureTime().split(":");

            LocalTime departureTime = LocalTime.of(Integer.parseInt(departures[0]), Integer.parseInt(departures[1]));

            LocalDateTime departureDate = LocalDateTime.of(LocalDate.now(), departureTime);

            Duration duration = Duration.between(nowDate, DateUtils.shanghaiToUTC(departureDate));

            long minute = duration.toMinutes();

            if (minute > 0) {
                tripsTruckDriverDTO = tripsTruckDriverDTOS.stream().collect(Collectors.toMap(TripsTruckDriverDTO::getTripsId, Function.identity(), (dto1, dto2) -> dto1)).get(item.getId());
                log.info("已找到最优方案");
                break;
            }
        }
        if (tripsTruckDriverDTO == null) {
            tripsTruckDriverDTO = tripsTruckDriverDTOS.get(0);
            log.info("未找到最优方案，使用第一种方案");
        }
        return tripsTruckDriverDTO;
    }

    /**
     * 获取全部可以用车次 车辆 司机
     *
     * @param transportTripsTruckDriverDtos
     * @return
     */
    private List<TripsTruckDriverDTO> getTripsTruckDriver(List<TransportTripsTruckDriverDto> transportTripsTruckDriverDtos) {

        Map<String, List<TransportTripsTruckDriverDto>> transportTripsTruckDriverDtoMap = transportTripsTruckDriverDtos.stream().collect(Collectors.groupingBy(TransportTripsTruckDriverDto::getTransportTripsId));

        List<TripsTruckDriverDTO> tripsTruckDriverDTOS = new ArrayList<>();
        transportTripsTruckDriverDtoMap.forEach((tripsId, transportTripsTruckDriverDtoList) -> {

            // 获取全部车辆 判断是否有可用车辆
            List<TruckDto> truckDtos = truckFeign.findAll(transportTripsTruckDriverDtoList.stream().filter(item -> StringUtils.isNotBlank(item.getTruckId())).map(item -> item.getTruckId()).collect(Collectors.toList()), null);
            // 获取状态正常的车辆id
            List<String> truckIds = truckDtos.stream().filter(item -> item.getStatus() == 1).map(item -> item.getId()).collect(Collectors.toList());

            // 获取全部司机id（xfwl-base 边界 userId 为 String，调 xfwl-auth 前转 Long）
            List<Long> driverIds = transportTripsTruckDriverDtoList.stream()
                .filter(item -> StringUtils.isNotBlank(item.getUserId()))
                .map(item -> IdConvert.toLong(item.getUserId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            List<UserDTO> users = userFeign.list(driverIds, null, null, null);
            // 远程失败(fallback 返回 null)按无可用司机降级，避免 NPE
            if (users == null) {
                users = new ArrayList<>();
            }
            // 正常状态司机id
            List<String> userIds = users.stream().filter(item -> Boolean.TRUE.equals(item.getStatus())).map(item -> item.getId().toString()).collect(Collectors.toList());

            transportTripsTruckDriverDtoList.forEach(item -> {
                if (userIds.contains(item.getUserId()) && truckIds.contains(item.getTruckId())) {
                    // 可用车次 车辆  司机  添加到模型中（tripsId/truckId 保持 xfwl-base String，driverId 内部 Long）
                    TripsTruckDriverDTO tripsTruckDriverDTO = new TripsTruckDriverDTO(tripsId, item.getTruckId(), IdConvert.toLong(item.getUserId()));
                    tripsTruckDriverDTOS.add(tripsTruckDriverDTO);
                }
            });
        });
        // FR-36 离线禁排班：剔除 GPS 离线车辆（gps.offline.timeout.minutes，默认 30 分钟无心跳即离线）；
        // 在线状态查询失败时降级为不过滤，不阻断调度主流程
        Set<String> onlineTruckIds = resolveOnlineTruckIds(
                tripsTruckDriverDTOS.stream().map(TripsTruckDriverDTO::getTruckId).collect(Collectors.toSet()));
        List<TripsTruckDriverDTO> onlineList = tripsTruckDriverDTOS.stream()
                .filter(item -> onlineTruckIds.contains(item.getTruckId()))
                .collect(Collectors.toList());
        if (onlineList.size() < tripsTruckDriverDTOS.size()) {
            log.warn("[FR-36] 离线禁排班：候选 {} 台中 {} 台 GPS 离线被剔除，剩 {} 台",
                    tripsTruckDriverDTOS.size(), tripsTruckDriverDTOS.size() - onlineList.size(), onlineList.size());
        }
        return onlineList;
    }

    /**
     * 判定在线车辆集合（FR-36）：车辆最近 GPS 心跳距现在 ≤ gps.offline.timeout.minutes 视为在线。
     *
     * <p>数据源 = xfwl-netty /trace/latestAll(type=1 车辆)；查询异常时降级为全放行（不阻断调度）。
     * 心跳时间解析失败的条目按在线处理（保守放行，仅剔除明确离线的）。</p>
     */
    private Set<String> resolveOnlineTruckIds(Set<String> truckIds) {
        if (truckIds == null || truckIds.isEmpty()) {
            return java.util.Collections.emptySet();
        }
        try {
            com.xinfei.common.utils.Result r = nettyTraceFeign.latestAll("1");
            if (r == null || r.get("data") == null) {
                log.warn("[FR-36] 车辆最新位置查询为空，降级为不过滤");
                return truckIds;
            }
            int offlineMinutes = dynamicConfigService.getInt("gps.offline.timeout.minutes", 30);
            java.time.LocalDateTime deadline = java.time.LocalDateTime.now().minusMinutes(offlineMinutes);
            Set<String> online = new java.util.HashSet<>();
            for (Object rowObj : (List<?>) r.get("data")) {
                if (!(rowObj instanceof Map)) {
                    continue;
                }
                Map<?, ?> row = (Map<?, ?>) rowObj;
                String businessId = String.valueOf(row.get("businessId"));
                if (!truckIds.contains(businessId)) {
                    continue;
                }
                Object rt = row.get("reportTime");
                if (rt == null) {
                    online.add(businessId); // 无心跳记录的保守放行
                    continue;
                }
                try {
                    String s = String.valueOf(rt).replace(" ", "T");
                    if (s.length() > 29) {
                        s = s.substring(0, 29); // 截掉纳秒超长部分
                    }
                    java.time.LocalDateTime last = java.time.LocalDateTime.parse(s);
                    if (last.isAfter(deadline)) {
                        online.add(businessId);
                    }
                } catch (Exception pe) {
                    log.debug("[FR-36] 心跳时间解析失败按在线处理: businessId={}, reportTime={}", businessId, rt);
                    online.add(businessId);
                }
            }
            return online;
        } catch (Exception e) {
            log.warn("[FR-36] 车辆在线状态查询异常，降级为不过滤（不阻断调度）: {}", e.getMessage());
            return truckIds;
        }
    }
}
