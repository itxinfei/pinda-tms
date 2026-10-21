package com.xinfei.auth.controller.v1;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xinfei.auth.common.v1.ApiResult;
import com.xinfei.auth.common.v1.DeptForm;
import com.xinfei.auth.common.v1.DeptVO;
import com.xinfei.auth.common.v1.OptionVO;
import com.xinfei.auth.service.v1.DeptService;
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
 * 部门管理（youlai 契约 /api/v1/depts）。
 */
@RestController
@RequestMapping("/depts")
@RequiredArgsConstructor
public class DeptController {

    private final DeptService deptService;

    @GetMapping
    @SaCheckPermission("sys:dept:list")
    public ApiResult<List<DeptVO>> list(
            @RequestParam(value = "keywords", required = false) String keywords,
            @RequestParam(value = "status", required = false) Integer status) {
        return ApiResult.success(deptService.tree(keywords, status));
    }

    @GetMapping("/options")
    @SaCheckPermission("sys:dept:list")
    public ApiResult<List<OptionVO>> options() {
        return ApiResult.success(deptService.options());
    }

    @GetMapping("/{id}/form")
    @SaCheckPermission("sys:dept:list")
    public ApiResult<DeptForm> form(@PathVariable Long id) {
        return ApiResult.success(deptService.form(id));
    }

    @PostMapping
    @SaCheckPermission("sys:dept:create")
    public ApiResult<Long> create(@RequestBody DeptForm form) {
        return ApiResult.success(deptService.create(form));
    }

    @PutMapping("/{id}")
    @SaCheckPermission("sys:dept:update")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody DeptForm form) {
        deptService.update(id, form);
        return ApiResult.success();
    }

    @DeleteMapping("/{ids}")
    @SaCheckPermission("sys:dept:delete")
    public ApiResult<Void> delete(@PathVariable String ids) {
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).map(Long::valueOf).toList();
        deptService.deleteByIds(idList);
        return ApiResult.success();
    }
}