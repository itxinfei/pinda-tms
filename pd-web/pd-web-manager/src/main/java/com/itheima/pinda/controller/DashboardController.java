package com.itheima.pinda.controller;

import com.itheima.pinda.DTO.TaskTransportDTO;
import com.itheima.pinda.DTO.dashboard.OrderOverviewDTO;
import com.itheima.pinda.DTO.dashboard.OrderTrendDTO;
import com.itheima.pinda.DTO.dashboard.PendingOrderDTO;
import com.itheima.pinda.common.utils.PageResponse;
import com.itheima.pinda.enums.transporttask.TransportTaskStatus;
import com.itheima.pinda.feign.OrderFeign;
import com.itheima.pinda.feign.TransportTaskFeign;
import com.itheima.pinda.vo.dashboard.DashboardOverviewVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

/**
 * 管理端工作台聚合接口（BFF，不连库）。
 *
 * <p>订单统计来自 pd-oms；在途车辆数复用 pd-work 既有运输任务分页接口读进行中任务计数，
 * 不为本页新增 work 端端点。异常告警由前端直连 pd-netty /alarm 获取，不经过本服务。
 * 返回值由 GlobalResponseAdvice 统一包成 {code,msg,data}。</p>
 */
@Slf4j
@RestController
@Tag(name = "工作台聚合API")
@RequestMapping("dashboard")
public class DashboardController {

    @Autowired
    private OrderFeign orderFeign;
    @Autowired
    private TransportTaskFeign transportTaskFeign;

    @Operation(summary = "工作台总览（KPI + 运单三桶 + 在途车辆）")
    @GetMapping("/overview")
    public DashboardOverviewVo overview() {
        OrderOverviewDTO orderOverview = orderFeign.dashboardOverview();
        DashboardOverviewVo vo = new DashboardOverviewVo();
        // oms 熔断时 fallback 返回全 0 对象（非 null），这里仍做空值防御
        vo.setTodayOrderCount(orderOverview != null && orderOverview.getTodayCount() != null
                ? orderOverview.getTodayCount() : 0L);
        vo.setPendingCount(orderOverview != null && orderOverview.getPendingCount() != null
                ? orderOverview.getPendingCount() : 0L);
        vo.setInTransitCount(orderOverview != null && orderOverview.getInTransitCount() != null
                ? orderOverview.getInTransitCount() : 0L);
        vo.setCompletedCount(orderOverview != null && orderOverview.getCompletedCount() != null
                ? orderOverview.getCompletedCount() : 0L);
        vo.setOnTruckCount(countProcessingTasks());
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
     * 在途车辆数 = 进行中（status=2）运输任务数。每辆车至多有一条进行中干线任务，
     * 以此近似"在途车辆"，比 GPS 在线数更贴业务语义且不依赖心跳。
     * 只取第 1 页 1 条，读分页 total(counts) 即可。
     */
    private long countProcessingTasks() {
        TaskTransportDTO query = new TaskTransportDTO();
        query.setStatus(TransportTaskStatus.PROCESSING.getCode());
        query.setPage(1);
        query.setPageSize(1);
        PageResponse<TaskTransportDTO> page = transportTaskFeign.findByPage(query);
        return page != null && page.getCounts() != null ? page.getCounts() : 0L;
    }
}
