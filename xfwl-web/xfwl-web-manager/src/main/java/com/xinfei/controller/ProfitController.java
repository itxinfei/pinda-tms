package com.xinfei.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xinfei.common.utils.Result;
import com.xinfei.feign.ProfitFeign;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端利润核算—— 薄转发至 xfwl-work，网关前缀 /api/manager/profit。
 *
 * <p>P0-6d/FIN-4 的网关暴露层（V1 只做毛利；drill 钻取 P1）。
 * 权限码 {@code finance:profit:*}。</p>
 */
@RestController
@RequestMapping("profit")
@RequiredArgsConstructor
@Tag(name = "利润核算")
public class ProfitController {

    private final ProfitFeign profitFeign;

    @Operation(summary = "单趟利润")
    @GetMapping("/task/{taskId}")
    @SaCheckPermission("finance:profit:*")
    public Result taskProfit(@PathVariable("taskId") Long taskId) {
        return profitFeign.taskProfit(taskId);
    }

    @Operation(summary = "单订单利润(V1 均摊)")
    @GetMapping("/order/{orderId}")
    @SaCheckPermission("finance:profit:*")
    public Result orderProfit(@PathVariable("orderId") Long orderId) {
        return profitFeign.orderProfit(orderId);
    }

    @Operation(summary = "利润汇总(按月)")
    @GetMapping("/summary")
    @SaCheckPermission("finance:profit:*")
    public Result summary() {
        return profitFeign.summary();
    }

    @Operation(summary = "利润排行(最赚/最亏)")
    @GetMapping("/ranking")
    @SaCheckPermission("finance:profit:*")
    public Result ranking() {
        return profitFeign.ranking();
    }
}
