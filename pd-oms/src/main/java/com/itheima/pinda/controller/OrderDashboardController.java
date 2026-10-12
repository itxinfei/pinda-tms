package com.itheima.pinda.controller;

import com.itheima.pinda.DTO.dashboard.OrderOverviewDTO;
import com.itheima.pinda.DTO.dashboard.OrderTrendDTO;
import com.itheima.pinda.DTO.dashboard.PendingOrderDTO;
import com.itheima.pinda.service.IOrderDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端工作台-订单统计（只读）。
 *
 * <p>类路径为 order/dashboard，配合 OrderFeign 的 path=/order 前缀，
 * Feign 方法 /dashboard/** 拼出 /order/dashboard/**，与本控制器路径一致。
 * 域服务裸返回 DTO/List（不经 BFF 响应壳），Feign 按同类型反序列化。</p>
 */
@Slf4j
@RestController
@Tag(name = "工作台订单统计API")
@RequestMapping("order/dashboard")
public class OrderDashboardController {

    @Autowired
    private IOrderDashboardService orderDashboardService;

    @Operation(summary = "订单总览（今日/待处理/运输中/已签收）")
    @GetMapping("/overview")
    public OrderOverviewDTO overview() {
        return orderDashboardService.overview();
    }

    @Operation(summary = "近N日运输量趋势")
    @GetMapping("/trend")
    public List<OrderTrendDTO> trend(@RequestParam(name = "days", defaultValue = "7") Integer days) {
        return orderDashboardService.trend(days);
    }

    @Operation(summary = "最新待处理运单")
    @GetMapping("/pending")
    public List<PendingOrderDTO> pending(@RequestParam(name = "limit", defaultValue = "5") Integer limit) {
        return orderDashboardService.pendingOrders(limit);
    }
}
