package com.xinfei.auth.controller.internal;

import com.xinfei.auth.service.internal.RoleQueryService;
import com.xinfei.DTO.RoleDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 角色内部端点：路径与 RoleFeign 对齐。
 *
 * <p>IDOR 防护（T-SEC-09/A6）：/internal/** 路径不经网关暴露（网关无此路由），
 * 直连端口由 xf-common SameTokenAuthInterceptor 保护（T-SEC-04）——
 * 仅携带合法 Same-Token 的内部服务调用可访问，外部伪造请求 401 拒绝。</p>
 */
@RestController
@RequestMapping("/internal/role")
@RequiredArgsConstructor
public class RoleInternalController {

    private final RoleQueryService roleQueryService;

    @GetMapping("/list")
    public List<RoleDTO> list(@RequestParam("userId") Long userId) {
        return roleQueryService.list(userId);
    }
}
