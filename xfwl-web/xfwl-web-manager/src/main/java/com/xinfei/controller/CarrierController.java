package com.xinfei.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xinfei.common.utils.Result;
import com.xinfei.feign.carrier.CarrierFeign;
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

import java.math.BigDecimal;
import java.util.Map;

/**
 * 管理端承运商/外协运力池（DSP-2 第一期 / CAP-01）—— 薄转发至 xfwl-base，
 * 网关前缀 /api/manager/carrier。
 *
 * <p>权限码用具体码（禁通配：Sa-Token 通配仅对持通配权限的用户生效，
 * 持具体码的普通角色会被误拦 403，2026-10-19 决策留痕口径）。</p>
 */
@RestController
@RequestMapping("carrier")
@RequiredArgsConstructor
@Tag(name = "承运商/外协运力池")
public class CarrierController {

    private final CarrierFeign carrierFeign;

    @Operation(summary = "新增/修改承运商")
    @PostMapping
    @SaCheckPermission("base:carrier:save")
    public Result save(@RequestBody Map<String, Object> body) {
        return carrierFeign.save(body);
    }

    @Operation(summary = "分页查询")
    @GetMapping("/page")
    @SaCheckPermission("base:carrier:page")
    public Result page(@RequestParam(value = "page", required = false) Integer page,
                       @RequestParam(value = "pageSize", required = false) Integer pageSize,
                       @RequestParam(value = "code", required = false) String code,
                       @RequestParam(value = "name", required = false) String name,
                       @RequestParam(value = "status", required = false) Integer status) {
        return carrierFeign.page(page, pageSize, code, name, status);
    }

    @Operation(summary = "详情（含挂靠车辆/司机）")
    @GetMapping("/{id}")
    @SaCheckPermission("base:carrier:detail")
    public Result detail(@PathVariable("id") Long id) {
        return carrierFeign.detail(id);
    }

    @Operation(summary = "状态流转（1合作中/0暂停/2黑名单）")
    @PostMapping("/{id}/status")
    @SaCheckPermission("base:carrier:disable")
    public Result changeStatus(@PathVariable("id") Long id, @RequestParam("status") Integer status) {
        return carrierFeign.changeStatus(id, status);
    }

    @Operation(summary = "挂靠车辆新增/修改")
    @PostMapping("/truck")
    @SaCheckPermission("base:carrier:save")
    public Result saveTruck(@RequestBody Map<String, Object> body) {
        return carrierFeign.saveTruck(body);
    }

    @Operation(summary = "挂靠司机新增/修改")
    @PostMapping("/driver")
    @SaCheckPermission("base:carrier:save")
    public Result saveDriver(@RequestBody Map<String, Object> body) {
        return carrierFeign.saveDriver(body);
    }

    @Operation(summary = "运力池查询（合作中承运商的正常车辆）")
    @GetMapping("/capacity")
    @SaCheckPermission("base:carrier:capacity")
    public Result capacity(@RequestParam(value = "keyword", required = false) String keyword,
                           @RequestParam(value = "truckTypeId", required = false) Long truckTypeId,
                           @RequestParam(value = "minLoad", required = false) BigDecimal minLoad) {
        return carrierFeign.capacity(keyword, truckTypeId, minLoad);
    }
}
