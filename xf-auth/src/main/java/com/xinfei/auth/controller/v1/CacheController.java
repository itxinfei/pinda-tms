package com.xinfei.auth.controller.v1;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xinfei.auth.common.v1.ApiResult;
import com.xinfei.auth.common.v1.CacheKeyVO;
import com.xinfei.auth.common.v1.PageResult;
import com.xinfei.auth.service.v1.CacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 缓存管理（youlai 契约风格 /api/v1/cache，USR-03）：键查看 + 单键删除。
 * 列表/删除各设独立按钮权限；删除动作由 OperLogInterceptor 记 OPER 审计。
 */
@RestController
@RequestMapping("/cache")
@RequiredArgsConstructor
public class CacheController {

    private final CacheService cacheService;

    @GetMapping
    @SaCheckPermission("sys:cache:list")
    public ApiResult<PageResult<CacheKeyVO>> page(
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize,
            @RequestParam(value = "pattern", required = false) String pattern) {
        return ApiResult.success(cacheService.page(pageNum, pageSize, pattern));
    }

    @DeleteMapping
    @SaCheckPermission("sys:cache:delete")
    public ApiResult<Void> delete(@RequestParam("key") String key) {
        cacheService.delete(key);
        return ApiResult.success();
    }
}
