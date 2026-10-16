package com.xinfei.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xinfei.common.utils.Result;
import com.xinfei.feign.DecisionLogFeign;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端合规决策留痕—— 薄转发至 xf-oms，网关前缀 /api/manager/decision-log。
 *
 * <p>P0-9/FR-75 的网关暴露层（改派/调价/拒单留痕查询与监管导出）。
 * 权限码 {@code finance:decision:*}。</p>
 */
@RestController
@RequestMapping("decision-log")
@RequiredArgsConstructor
@Tag(name = "合规决策留痕")
public class DecisionLogController {

    private final DecisionLogFeign decisionLogFeign;

    @Operation(summary = "落留痕")
    @PostMapping("/apply")
    @SaCheckPermission("finance:decision:*")
    public Result apply(@RequestParam("businessType") String businessType,
                        @RequestParam("businessId") Long businessId,
                        @RequestParam("reasonEnum") String reasonEnum,
                        @RequestParam(value = "detail", required = false) String detail) {
        return decisionLogFeign.apply(businessType, businessId, reasonEnum, detail);
    }

    @Operation(summary = "分页查询")
    @GetMapping("/page")
    @SaCheckPermission("finance:decision:*")
    public Result page(@RequestParam(value = "page", required = false) Integer page,
                       @RequestParam(value = "pageSize", required = false) Integer pageSize,
                       @RequestParam(value = "businessType", required = false) String businessType,
                       @RequestParam(value = "businessId", required = false) Long businessId) {
        return decisionLogFeign.page(page, pageSize, businessType, businessId);
    }

    @Operation(summary = "监管导出")
    @GetMapping("/export")
    @SaCheckPermission("finance:decision:*")
    public Result export(@RequestParam(value = "businessType", required = false) String businessType,
                         @RequestParam(value = "businessId", required = false) Long businessId,
                         @RequestParam(value = "beginTime", required = false) String beginTime,
                         @RequestParam(value = "endTime", required = false) String endTime) {
        return decisionLogFeign.export(businessType, businessId, beginTime, endTime);
    }
}
