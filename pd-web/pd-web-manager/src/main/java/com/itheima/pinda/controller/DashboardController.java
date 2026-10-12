package com.itheima.pinda.controller;

import com.itheima.pinda.DTO.dashboard.OrderOverviewDTO;
import com.itheima.pinda.DTO.dashboard.OrderTrendDTO;
import com.itheima.pinda.DTO.dashboard.PendingOrderDTO;
import com.itheima.pinda.DTO.dashboard.TaskDashboardSummaryDTO;
import com.itheima.pinda.DTO.dashboard.TripsVolumeDTO;
import com.itheima.pinda.DTO.transportline.TransportLineDto;
import com.itheima.pinda.DTO.transportline.TransportTripsDto;
import com.itheima.pinda.feign.OrderFeign;
import com.itheima.pinda.feign.TransportTaskFeign;
import com.itheima.pinda.feign.transportline.TransportLineFeign;
import com.itheima.pinda.feign.transportline.TransportTripsFeign;
import com.itheima.pinda.feign.truck.TruckFeign;
import com.itheima.pinda.vo.dashboard.BigScreenSummaryVo;
import com.itheima.pinda.vo.dashboard.DashboardOverviewVo;
import com.itheima.pinda.vo.dashboard.LineVolumeVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理端工作台/大屏聚合接口（BFF，不连库）。
 *
 * <p>订单统计来自 pd-oms；运单/运输任务/车辆状态来自 pd-work（OPS-3：车辆状态由运输任务推导）；
 * 车辆总数来自 pd-base。异常告警与车辆 GPS 定位由前端直连 pd-netty，不经过本服务。
 * 返回值由 GlobalResponseAdvice 统一包成 {code,msg,data}。</p>
 */
@Slf4j
@RestController
@Tag(name = "工作台聚合API")
@RequestMapping("dashboard")
public class DashboardController {

    /**
     * 大屏「各线路运量」统计天数与 Top 条数（work 端同样限制 Top10）
     */
    private static final int LINE_VOLUME_DAYS = 7;

    @Autowired
    private OrderFeign orderFeign;
    @Autowired
    private TransportTaskFeign transportTaskFeign;
    @Autowired
    private TruckFeign truckFeign;
    @Autowired
    private TransportTripsFeign transportTripsFeign;
    @Autowired
    private TransportLineFeign transportLineFeign;

    @Operation(summary = "工作台总览（KPI + 订单三桶 + 今日签收 + 在途车辆 + 任务五态）")
    @GetMapping("/overview")
    public DashboardOverviewVo overview() {
        OrderOverviewDTO orderOverview = orderFeign.dashboardOverview();
        TaskDashboardSummaryDTO taskSummary = transportTaskFeign.dashboardSummary();

        DashboardOverviewVo vo = new DashboardOverviewVo();
        // oms/base 熔断时 fallback 返回全 0 对象（非 null），这里仍做空值防御
        vo.setTodayOrderCount(nvl(orderOverview == null ? null : orderOverview.getTodayCount()));
        vo.setPendingCount(nvl(orderOverview == null ? null : orderOverview.getPendingCount()));
        vo.setInTransitCount(nvl(orderOverview == null ? null : orderOverview.getInTransitCount()));
        vo.setCompletedCount(nvl(orderOverview == null ? null : orderOverview.getCompletedCount()));
        vo.setTodayCompletedCount(nvl(orderOverview == null ? null : orderOverview.getTodayCompletedCount()));
        vo.setOnTruckCount(nvl(taskSummary == null ? null : taskSummary.getInTransitTruckCount()));
        vo.setTodayTaskCount(nvl(taskSummary == null ? null : taskSummary.getTodayTaskCount()));
        vo.setTaskStatusCounts(taskSummary == null || taskSummary.getTaskStatusCounts() == null
                ? Collections.emptyList() : taskSummary.getTaskStatusCounts());
        return vo;
    }

    @Operation(summary = "运营大屏汇总（4 KPI 所需后端数据 + 任务五态 + 线路运量 Top10）")
    @GetMapping("/bigscreen/summary")
    public BigScreenSummaryVo bigScreenSummary() {
        OrderOverviewDTO orderOverview = orderFeign.dashboardOverview();
        TaskDashboardSummaryDTO taskSummary = transportTaskFeign.dashboardSummary();

        BigScreenSummaryVo vo = new BigScreenSummaryVo();
        vo.setTodayOrderCount(nvl(orderOverview == null ? null : orderOverview.getTodayCount()));

        if (taskSummary != null) {
            vo.setTodayTransportOrderCount(nvl(taskSummary.getTodayTransportOrderCount()));
            vo.setTodayTaskCount(nvl(taskSummary.getTodayTaskCount()));
            vo.setActiveTruckCount(nvl(taskSummary.getInTransitTruckCount()));
            vo.setPendingTruckCount(nvl(taskSummary.getPendingTruckCount()));
            vo.setTaskStatusCounts(taskSummary.getTaskStatusCounts() == null
                    ? Collections.emptyList() : taskSummary.getTaskStatusCounts());
        } else {
            vo.setTodayTransportOrderCount(0L);
            vo.setTodayTaskCount(0L);
            vo.setActiveTruckCount(0L);
            vo.setPendingTruckCount(0L);
            vo.setTaskStatusCounts(Collections.emptyList());
        }

        // 空闲车辆 = 正常车辆总数 - 在途 - 待执行（OPS-3，下限 0）
        Integer totalTrucks = truckFeign.count(null);
        long total = totalTrucks != null ? totalTrucks : 0L;
        vo.setTotalTruckCount(total);
        long idle = total - vo.getActiveTruckCount() - vo.getPendingTruckCount();
        vo.setIdleTruckCount(Math.max(0L, idle));

        vo.setLineVolumes(buildLineVolumes());
        return vo;
    }

    @Operation(summary = "近N日运输量趋势")
    @GetMapping("/trend")
    public List<OrderTrendDTO> trend(@RequestParam(name = "days", defaultValue = "7") Integer days) {
        List<OrderTrendDTO> trend = orderFeign.dashboardTrend(days);
        return trend == null ? Collections.emptyList() : trend;
    }

    @Operation(summary = "最新待处理运单")
    @GetMapping("/todo")
    public List<PendingOrderDTO> todo(@RequestParam(name = "limit", defaultValue = "5") Integer limit) {
        List<PendingOrderDTO> todo = orderFeign.dashboardPending(limit);
        return todo == null ? Collections.emptyList() : todo;
    }

    /**
     * 近 7 日各线路运量 Top10：work 端只给车次 id + 任务量，
     * 经 base 补车次名→线路名；任一远程熔断降级为空列表/空 Map，不影响大屏其余数据。
     */
    private List<LineVolumeVo> buildLineVolumes() {
        List<TripsVolumeDTO> volumes = transportTaskFeign.dashboardTripsVolume(LINE_VOLUME_DAYS);
        if (volumes == null || volumes.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> tripsIds = volumes.stream()
                .map(TripsVolumeDTO::getTripsId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        Map<String, TransportTripsDto> tripsMap = new HashMap<>();
        if (!tripsIds.isEmpty()) {
            List<TransportTripsDto> tripsList = transportTripsFeign.findAll(null, tripsIds);
            if (tripsList != null) {
                for (TransportTripsDto trips : tripsList) {
                    if (trips.getId() != null) {
                        tripsMap.put(trips.getId(), trips);
                    }
                }
            }
        }

        // 车次 → 线路 id 去重收集，再批量取线路名（最多两次 base 调用）
        Set<String> lineIds = new LinkedHashSet<>();
        for (TransportTripsDto trips : tripsMap.values()) {
            if (StringUtils.hasText(trips.getTransportLineId())) {
                lineIds.add(trips.getTransportLineId());
            }
        }
        Map<String, String> lineNameMap = new HashMap<>();
        if (!lineIds.isEmpty()) {
            List<TransportLineDto> lines = transportLineFeign.findAll(new ArrayList<>(lineIds), null, null);
            if (lines != null) {
                for (TransportLineDto line : lines) {
                    if (line.getId() != null && StringUtils.hasText(line.getName())) {
                        lineNameMap.put(line.getId(), line.getName());
                    }
                }
            }
        }

        List<LineVolumeVo> result = new ArrayList<>(volumes.size());
        for (TripsVolumeDTO volume : volumes) {
            String name = resolveLineName(volume.getTripsId(), tripsMap, lineNameMap);
            result.add(new LineVolumeVo(name, nvl(volume.getTaskCount())));
        }
        return result;
    }

    /**
     * 线路名优先级：线路名称 → 车次名称 → 「车次 {id}」兜底，保证柱状图标签不为空。
     */
    private String resolveLineName(String tripsId, Map<String, TransportTripsDto> tripsMap,
                                   Map<String, String> lineNameMap) {
        TransportTripsDto trips = tripsMap.get(tripsId);
        if (trips != null) {
            if (StringUtils.hasText(trips.getTransportLineId())) {
                String lineName = lineNameMap.get(trips.getTransportLineId());
                if (StringUtils.hasText(lineName)) {
                    return lineName;
                }
            }
            if (StringUtils.hasText(trips.getName())) {
                return trips.getName();
            }
        }
        return "车次 " + tripsId;
    }

    private Long nvl(Long value) {
        return value != null ? value : 0L;
    }
}
