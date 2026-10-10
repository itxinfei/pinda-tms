package com.itheima.auth.service.internal;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.itheima.auth.entity.AuthUserRole;
import com.itheima.auth.entity.Role;
import com.itheima.auth.mapper.AuthUserRoleMapper;
import com.itheima.auth.mapper.RoleMapper;
import com.itheima.pinda.DTO.RoleDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色内部查询：供 /internal/role 端点调用，返回裸 DTO。
 */
@Service
@RequiredArgsConstructor
public class RoleQueryService {

    private final RoleMapper roleMapper;
    private final AuthUserRoleMapper userRoleMapper;

    /**
     * 查询指定用户拥有的启用角色
     */
    public List<RoleDTO> list(Long userId) {
        List<AuthUserRole> links = userRoleMapper.selectList(Wrappers.<AuthUserRole>lambdaQuery()
                .eq(AuthUserRole::getUserId, userId));
        if (links.isEmpty()) {
            return List.of();
        }
        List<Long> roleIds = links.stream()
                .map(AuthUserRole::getRoleId)
                .distinct()
                .collect(Collectors.toList());
        List<Role> roles = roleMapper.selectList(Wrappers.<Role>lambdaQuery()
                .in(Role::getId, roleIds)
                .eq(Role::getStatus, 1));
        return roles.stream().map(role -> {
            RoleDTO dto = new RoleDTO();
            dto.setId(role.getId());
            dto.setName(role.getName());
            return dto;
        }).collect(Collectors.toList());
    }
}
