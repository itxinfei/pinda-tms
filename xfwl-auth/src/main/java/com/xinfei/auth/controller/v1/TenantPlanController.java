package com.xinfei.auth.controller.v1;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xinfei.auth.common.v1.ApiResult;
import com.xinfei.auth.common.v1.OptionVO;
import com.xinfei.auth.common.v1.PageResult;
import com.xinfei.auth.common.v1.TenantPlanForm;
import com.xinfei.auth.common.v1.TenantPlanVO;
import com.xinfei.auth.service.v1.TenantPlanService;
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
 * 租户套餐管理（youlai 契约 /api/v1/tenant-plans）。
 */
@RestController
@RequestMapping("/tenant-plans")
@RequiredArgsConstructor
public class TenantPlanController {

    private final TenantPlanService tenantPlanService;

    @GetMapping
    @SaCheckPermission("sys:tenant:list")
    public ApiResult<PageResult<TenantPlanVO>> page(
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize,
            @RequestParam(value = "keywords", required = false) String keywords,
            @RequestParam(value = "status", required = false) Integer status) {
        return ApiResult.success(tenantPlanService.page(pageNum, pageSize, keywords, status));
    }

    @GetMapping("/options")
    @SaCheckPermission("sys:tenant:list")
    public ApiResult<List<OptionVO>> options() {
        return ApiResult.success(tenantPlanService.options());
    }

    @GetMapping("/{id}/form")
    @SaCheckPermission("sys:tenant:list")
    public ApiResult<TenantPlanForm> form(@PathVariable Long id) {
        return ApiResult.success(tenantPlanService.form(id));
    }

    @GetMapping("/{id}/menuIds")
    @SaCheckPermission("sys:tenant:list")
    public ApiResult<List<Long>> menuIds(@PathVariable Long id) {
        return ApiResult.success(tenantPlanService.getMenuIds(id));
    }

    @PostMapping
    @SaCheckPermission("sys:tenant:create")
    public ApiResult<Long> create(@RequestBody TenantPlanForm form) {
        return ApiResult.success(tenantPlanService.create(form));
    }

    @PutMapping("/{id}")
    @SaCheckPermission("sys:tenant:update")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody TenantPlanForm form) {
        tenantPlanService.update(id, form);
        return ApiResult.success();
    }

    @PutMapping("/{id}/menus")
    @SaCheckPermission("sys:tenant:plan-assign")
    public ApiResult<Void> updateMenus(@PathVariable Long id, @RequestBody List<Long> menuIds) {
        tenantPlanService.updateMenus(id, menuIds);
        return ApiResult.success();
    }

    @DeleteMapping("/{ids}")
    @SaCheckPermission("sys:tenant:delete")
    public ApiResult<Void> delete(@PathVariable String ids) {
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).map(Long::valueOf).toList();
        tenantPlanService.deleteByIds(idList);
        return ApiResult.success();
    }
}