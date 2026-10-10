package com.itheima.auth.controller.internal;

import com.itheima.auth.service.internal.RoleQueryService;
import com.itheima.pinda.DTO.RoleDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 角色内部端点：路径与 RoleFeign 对齐。
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
