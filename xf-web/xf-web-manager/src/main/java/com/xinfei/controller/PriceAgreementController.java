package com.xinfei.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xinfei.common.utils.Result;
import com.xinfei.feign.PriceAgreementFeign;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理端客户价格协议（FIN-1.1 / POD-06 客户侧费率卡）—— 薄转发至 xf-oms，
 * 网关前缀 /api/manager/price-agreement。
 *
 * <p>权限码用具体码（禁通配：Sa-Token 通配仅对持通配权限的用户生效，
 * 持具体码的普通角色会被误拦 403，2026-10-19 决策留痕口径）。</p>
 */
@RestController
@RequestMapping("price-agreement")
@RequiredArgsConstructor
@Tag(name = "客户价格协议(费率卡)")
public class PriceAgreementController {

    private final PriceAgreementFeign priceAgreementFeign;

    @Operation(summary = "新增/修改协议")
    @PostMapping
    @SaCheckPermission("finance:price-agreement:save")
    public Result save(@RequestBody Map<String, Object> body) {
        return priceAgreementFeign.save(body);
    }

    @Operation(summary = "停用协议")
    @PostMapping("/{id}/disable")
    @SaCheckPermission("finance:price-agreement:disable")
    public Result disable(@PathVariable("id") Long id) {
        return priceAgreementFeign.disable(id);
    }

    @Operation(summary = "分页查询")
    @GetMapping("/page")
    @SaCheckPermission("finance:price-agreement:page")
    public Result page(@RequestParam(value = "page", required = false) Integer page,
                       @RequestParam(value = "pageSize", required = false) Integer pageSize,
                       @RequestParam(value = "memberId", required = false) String memberId,
                       @RequestParam(value = "status", required = false) Integer status) {
        return priceAgreementFeign.page(page, pageSize, memberId, status);
    }
}
