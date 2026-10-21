package com.xinfei.auth.controller.v1;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xinfei.auth.common.v1.ApiResult;
import com.xinfei.auth.common.v1.OnlineVO;
import com.xinfei.auth.common.v1.PageResult;
import com.xinfei.auth.service.v1.OnlineService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 在线用户（youlai 契约风格 /api/v1/online）：会话列表 + 强制下线。
 * 列表/踢人各设独立按钮权限；踢人动作由 OperLogInterceptor 记 OPER 审计。
 */
@RestController
@RequestMapping("/online")
@RequiredArgsConstructor
public class OnlineController {

    private final OnlineService onlineService;

    @GetMapping
    @SaCheckPermission("sys:online:list")
    public ApiResult<PageResult<OnlineVO>> page(
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize,
            @RequestParam(value = "keywords", required = false) String keywords) {
        return ApiResult.success(onlineService.page(pageNum, pageSize, keywords));
    }

    @DeleteMapping("/{token}")
    @SaCheckPermission("sys:online:kickout")
    public ApiResult<Void> kickout(@PathVariable String token) {
        onlineService.kickout(token);
        return ApiResult.success();
    }
}
