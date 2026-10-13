package com.itheima.auth.controller.v1;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.itheima.auth.common.v1.ApiResult;
import com.itheima.auth.common.v1.OptionVO;
import com.itheima.auth.common.v1.PageResult;
import com.itheima.auth.common.v1.RoleForm;
import com.itheima.auth.common.v1.RoleVO;
import com.itheima.auth.service.v1.RoleService;
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
 * 角色管理（youlai 契约 /api/v1/roles）。
 * 菜单授权 menu-ids/menus 已实现；dept-ids 数据权限 P2 固定空。
 */
@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @SaCheckPermission("sys:role:list")
    public ApiResult<PageResult<RoleVO>> page(
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize,
            @RequestParam(value = "keywords", required = false) String keywords) {
        return ApiResult.success(roleService.page(pageNum, pageSize, keywords));
    }

    @GetMapping("/options")
    @SaCheckPermission("sys:role:list")
    public ApiResult<List<OptionVO>> options() {
        return ApiResult.success(roleService.options());
    }

    @GetMapping("/code-options")
    @SaCheckPermission("sys:role:list")
    public ApiResult<List<OptionVO>> codeOptions() {
        return ApiResult.success(roleService.codeOptions());
    }

    @GetMapping("/{id}/form")
    @SaCheckPermission("sys:role:list")
    public ApiResult<RoleForm> form(@PathVariable Long id) {
        return ApiResult.success(roleService.formData(id));
    }

    @GetMapping("/{id}/menu-ids")
    @SaCheckPermission("sys:role:list")
    public ApiResult<List<Long>> menuIds(@PathVariable Long id) {
        return ApiResult.success(roleService.menuIds(id));
    }

    @PutMapping("/{id}/menus")
    @SaCheckPermission("sys:role:edit")
    public ApiResult<Void> updateMenus(@PathVariable Long id, @RequestBody List<Long> menuIds) {
        roleService.updateMenus(id, menuIds);
        return ApiResult.success();
    }

    /** P2 数据权限：本期固定空数组（=全部数据） */
    @GetMapping("/{id}/dept-ids")
    @SaCheckPermission("sys:role:list")
    public ApiResult<List<Long>> deptIds(@PathVariable Long id) {
        return ApiResult.success(roleService.deptIds(id));
    }

    @PostMapping
    @SaCheckPermission("sys:role:add")
    public ApiResult<Long> create(@RequestBody RoleForm form) {
        return ApiResult.success(roleService.create(form));
    }

    @PutMapping("/{id}")
    @SaCheckPermission("sys:role:edit")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody RoleForm form) {
        roleService.update(id, form);
        return ApiResult.success();
    }

    @DeleteMapping("/{ids}")
    @SaCheckPermission("sys:role:delete")
    public ApiResult<Void> delete(@PathVariable String ids) {
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .map(Long::valueOf).toList();
        roleService.deleteByIds(idList);
        return ApiResult.success();
    }
}
