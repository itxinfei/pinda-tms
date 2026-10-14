package com.xinfei.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;

import com.xinfei.DTO.SysConfigDTO;
import com.xinfei.common.utils.Result;
import com.xinfei.feign.ConfigFeign;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端参数配置（CFG-01）：薄转发至 pd-auth，网关前缀 /api/manager。
 *
 * <p>面向前端统一包 {@code {code,msg,data}} 外壳（读端点 data 为分页/详情），
 * 以契合前端 request 拦截器 {@code code===0} 的成功判定；写操作直接透传下游 Result。</p>
 */
@RestController
@RequestMapping("sys-config")
@RequiredArgsConstructor
@Tag(name = "参数配置")
public class SysConfigController {

    private final ConfigFeign configFeign;

    @Operation(summary = "参数配置分页")
    @GetMapping("/page")
    @SaCheckPermission("config:list")
    public Result page(@RequestParam("page") Integer page,
                       @RequestParam("pageSize") Integer pageSize,
                       @RequestParam(value = "keywords", required = false) String keywords,
                       @RequestParam(value = "status", required = false) Integer status) {
        return Result.ok().put("data", configFeign.page(page, pageSize, keywords, status));
    }

    @Operation(summary = "参数配置详情")
    @GetMapping("/{id}")
    @SaCheckPermission("config:detail")
    public Result detail(@PathVariable("id") Long id) {
        return Result.ok().put("data", configFeign.detail(id));
    }

    @Operation(summary = "新增参数配置")
    @PostMapping
    @SaCheckPermission("config:create")
    public Result save(@RequestBody SysConfigDTO dto) {
        return configFeign.save(dto);
    }

    @Operation(summary = "修改参数配置")
    @PutMapping("/{id}")
    @SaCheckPermission("config:update")
    public Result update(@PathVariable("id") Long id, @RequestBody SysConfigDTO dto) {
        return configFeign.update(id, dto);
    }

    @Operation(summary = "删除参数配置")
    @DeleteMapping("/{id}")
    @SaCheckPermission("config:delete")
    public Result remove(@PathVariable("id") Long id) {
        return configFeign.remove(id);
    }
}
