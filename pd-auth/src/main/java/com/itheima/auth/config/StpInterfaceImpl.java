package com.itheima.auth.config;

import cn.dev33.satoken.stp.StpInterface;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.itheima.auth.entity.AuthUserRole;
import com.itheima.auth.entity.Role;
import com.itheima.auth.entity.SysMenu;
import com.itheima.auth.mapper.AuthUserRoleMapper;
import com.itheima.auth.mapper.RoleMapper;
import com.itheima.auth.mapper.RoleMenuMapper;
import com.itheima.auth.mapper.SysMenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Sa-Token 权限/角色数据源。
 *
 * <p>{@code @SaCheckPermission} / {@code @SaCheckRole} 经此聚合当前用户：
 * 角色取 sys_user_role → sys_role(code)；权限码取 角色 → sys_role_menu →
 * sys_menu(type=B 按钮的 perm，以及菜单自带 perm)。ADMIN 角色直接放行 {@code *}。</p>
 */
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    public static final String SUPER_ROLE = "ADMIN";
    public static final String ALL_PERMISSION = "*";

    private final AuthUserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final SysMenuMapper menuMapper;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        Long userId = parseId(loginId);
        if (userId == null) {
            return Collections.emptyList();
        }
        List<Role> roles = activeRoles(userId);
        if (roles.stream().anyMatch(r -> SUPER_ROLE.equalsIgnoreCase(r.getCode()))) {
            return List.of(ALL_PERMISSION);
        }
        List<Long> roleIds = roles.stream().map(Role::getId).toList();
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        // 角色 → 授权菜单/按钮 id（sys_role_menu 租户隔离由插件处理）
        List<Long> menuIds = userRoleMenuIds(roleIds);
        if (menuIds.isEmpty()) {
            return Collections.emptyList();
        }
        return menuMapper.selectList(Wrappers.<SysMenu>lambdaQuery()
                        .in(SysMenu::getId, menuIds)
                        .eq(SysMenu::getStatus, 1)
                        .isNotNull(SysMenu::getPerm))
                .stream()
                .map(SysMenu::getPerm)
                .filter(p -> p != null && !p.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Long userId = parseId(loginId);
        if (userId == null) {
            return Collections.emptyList();
        }
        return activeRoles(userId).stream()
                .map(Role::getCode)
                .filter(c -> c != null && !c.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }

    private List<Role> activeRoles(Long userId) {
        List<AuthUserRole> links = userRoleMapper.selectList(Wrappers.<AuthUserRole>lambdaQuery()
                .eq(AuthUserRole::getUserId, userId));
        if (links.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> roleIds = links.stream().map(AuthUserRole::getRoleId).distinct().toList();
        return roleMapper.selectList(Wrappers.<Role>lambdaQuery()
                .in(Role::getId, roleIds)
                .eq(Role::getStatus, 1));
    }

    /** 查角色授权的菜单/按钮 id */
    private List<Long> userRoleMenuIds(List<Long> roleIds) {
        return roleMenuMapper.selectMenuIdsByRoleIds(roleIds);
    }

    private Long parseId(Object loginId) {
        if (loginId == null) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(loginId));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 取用户授权菜单 id（供 /menus/routes 过滤用） */
    public List<Long> authorizedMenuIds(Long userId) {
        List<Role> roles = activeRoles(userId);
        if (roles.stream().anyMatch(r -> SUPER_ROLE.equalsIgnoreCase(r.getCode()))) {
            // 超管：null 表示不限制
            return null;
        }
        List<Long> roleIds = roles.stream().map(Role::getId).toList();
        if (roleIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> ids = roleMenuMapper.selectMenuIdsByRoleIds(roleIds);
        return ids == null ? new ArrayList<>() : ids;
    }

    /** 用户是否超管 */
    public boolean isSuperAdmin(Long userId) {
        return activeRoles(userId).stream().anyMatch(r -> SUPER_ROLE.equalsIgnoreCase(r.getCode()));
    }
}
