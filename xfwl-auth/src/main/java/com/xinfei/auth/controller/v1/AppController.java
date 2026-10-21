package com.xinfei.auth.controller.v1;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xinfei.auth.common.v1.ApiResult;
import com.xinfei.auth.common.v1.AppForm;
import com.xinfei.auth.common.v1.AppVO;
import com.xinfei.auth.common.v1.PageResult;
import com.xinfei.auth.service.v1.AppService;
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
 * 应用管理（youlai 契约 /api/v1/apps）。
 */
@RestController
@RequestMapping("/apps")
@RequiredArgsConstructor
public class AppController {

    private final AppService appService;

    @GetMapping
    @SaCheckPermission("sys:app:list")
    public ApiResult<PageResult<AppVO>> page(
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize,
            @RequestParam(value = "keywords", required = false) String keywords,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "platform", required = false) String platform) {
        return ApiResult.success(appService.page(pageNum, pageSize, keywords, status, platform));
    }

    @GetMapping("/{id}/form")
    @SaCheckPermission("sys:app:list")
    public ApiResult<AppForm> form(@PathVariable Long id) {
        return ApiResult.success(appService.form(id));
    }

    @PostMapping
    @SaCheckPermission("sys:app:create")
    public ApiResult<Long> create(@RequestBody AppForm form) {
        return ApiResult.success(appService.create(form));
    }

    @PutMapping("/{id}")
    @SaCheckPermission("sys:app:update")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody AppForm form) {
        appService.update(id, form);
        return ApiResult.success();
    }

    @DeleteMapping("/{ids}")
    @SaCheckPermission("sys:app:delete")
    public ApiResult<Void> delete(@PathVariable String ids) {
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).map(Long::valueOf).toList();
        appService.deleteByIds(idList);
        return ApiResult.success();
    }

    @PutMapping("/{id}/status")
    @SaCheckPermission("sys:app:change-status")
    public ApiResult<Void> updateStatus(@PathVariable Long id,
                                        @RequestParam("status") Integer status) {
        appService.updateStatus(id, status);
        return ApiResult.success();
    }
}