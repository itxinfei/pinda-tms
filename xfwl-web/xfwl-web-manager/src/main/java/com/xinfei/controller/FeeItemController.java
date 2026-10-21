package com.xinfei.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xinfei.common.utils.Result;
import com.xinfei.feign.FeeItemFeign;
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
 * 管理端附加费费用项配置（POD-03 第一期 / FA-10）—— 薄转发至 xfwl-oms，
 * 网关前缀 /api/manager/fee-item。权限码具体码禁通配。
 */
@RestController
@RequestMapping("fee-item")
@RequiredArgsConstructor
@Tag(name = "附加费费用项配置")
public class FeeItemController {

    private final FeeItemFeign feeItemFeign;

    @Operation(summary = "新增/修改费用项")
    @PostMapping
    @SaCheckPermission("finance:fee-item:save")
    public Result save(@RequestBody Map<String, Object> body) {
        return feeItemFeign.save(body);
    }

    @Operation(summary = "停用费用项")
    @PostMapping("/{id}/disable")
    @SaCheckPermission("finance:fee-item:disable")
    public Result disable(@PathVariable("id") Long id) {
        return feeItemFeign.disable(id);
    }

    @Operation(summary = "分页查询")
    @GetMapping("/page")
    @SaCheckPermission("finance:fee-item:page")
    public Result page(@RequestParam(value = "page", required = false) Integer page,
                       @RequestParam(value = "pageSize", required = false) Integer pageSize,
                       @RequestParam(value = "feeCode", required = false) String feeCode,
                       @RequestParam(value = "status", required = false) Integer status) {
        return feeItemFeign.page(page, pageSize, feeCode, status);
    }
}
