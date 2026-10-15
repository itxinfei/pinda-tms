package com.xinfei.auth.controller.v1;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xinfei.auth.common.v1.ApiResult;
import com.xinfei.auth.common.v1.PageResult;
import com.xinfei.auth.common.v1.TenantCreateForm;
import com.xinfei.auth.common.v1.TenantCreateResult;
import com.xinfei.auth.common.v1.TenantForm;
import com.xinfei.auth.common.v1.TenantInfo;
import com.xinfei.auth.common.v1.TenantVO;
import com.xinfei.auth.service.v1.TenantService;
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

import java.util.Arrays;
import java.util.List;

/**
 * 平台租户管理（youlai 契约 /api/v1/tenants）。
 */
@RestController
@RequestMapping("/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @GetMapping("/options")
    public ApiResult<List<TenantInfo>> options() {
        return ApiResult.success(tenantService.options());
    }

    @GetMapping("/current")
    public ApiResult<TenantInfo> current() {
        return ApiResult.success(tenantService.current());
    }

    @PostMapping("/{tenantId}/switch")
    public ApiResult<TenantInfo> switchTenant(@PathVariable Long tenantId) {
        return ApiResult.success(tenantService.switchTenant(tenantId));
    }

    @GetMapping
    @SaCheckPermission("sys:tenant:list")
    public ApiResult<PageResult<TenantVO>> page(
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize,
            @RequestParam(value = "keywords", required = false) String keywords,
            @RequestParam(value = "status", required = false) Integer status) {
        return ApiResult.success(tenantService.page(pageNum, pageSize, keywords, status));
    }

    @GetMapping("/{id}/form")
    @SaCheckPermission("sys:tenant:list")
    public ApiResult<TenantForm> form(@PathVariable Long id) {
        return ApiResult.success(tenantService.form(id));
    }

    @PostMapping
    @SaCheckPermission("sys:tenant:create")
    public ApiResult<TenantCreateResult> create(@RequestBody TenantCreateForm form) {
        return ApiResult.success(tenantService.create(form));
    }

    @PutMapping("/{id}")
    @SaCheckPermission("sys:tenant:update")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody TenantForm form) {
        tenantService.update(id, form);
        return ApiResult.success();
    }

    @DeleteMapping("/{ids}")
    @SaCheckPermission("sys:tenant:delete")
    public ApiResult<Void> delete(@PathVariable String ids) {
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).map(Long::valueOf).toList();
        tenantService.deleteByIds(idList);
        return ApiResult.success();
    }

    @PutMapping("/{id}/status")
    @SaCheckPermission("sys:tenant:update")
    public ApiResult<Void> updateStatus(@PathVariable Long id,
                                        @RequestParam("status") Integer status) {
        tenantService.updateStatus(id, status);
        return ApiResult.success();
    }

    @GetMapping("/{id}/menuIds")
    @SaCheckPermission("sys:tenant:list")
    public ApiResult<List<Long>> menuIds(@PathVariable Long id) {
        return ApiResult.success(tenantService.getMenuIds(id));
    }

    @PutMapping("/{id}/menus")
    @SaCheckPermission("sys:tenant:assign")
    public ApiResult<Void> updateMenus(@PathVariable Long id, @RequestBody List<Long> menuIds) {
        tenantService.updateMenus(id, menuIds);
        return ApiResult.success();
    }
}
