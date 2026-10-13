package com.itheima.auth.service.v1;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.auth.common.BizException;
import com.itheima.auth.common.v1.PageResult;
import com.itheima.auth.common.v1.UserForm;
import com.itheima.auth.common.v1.UserVO;
import com.itheima.auth.config.StpInterfaceImpl;
import com.itheima.auth.entity.AuthUser;
import com.itheima.auth.entity.AuthUserRole;
import com.itheima.auth.entity.CoreOrg;
import com.itheima.auth.entity.CoreStation;
import com.itheima.auth.entity.Role;
import com.itheima.auth.mapper.AuthUserMapper;
import com.itheima.auth.mapper.AuthUserRoleMapper;
import com.itheima.auth.mapper.CoreOrgMapper;
import com.itheima.auth.mapper.CoreStationMapper;
import com.itheima.auth.mapper.RoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理端用户管理（youlai 契约，sys_user / sys_user_role）。
 *
 * <p>字段适配：前端 username/nickname/gender/deptId ↔ 实体 account/name/sex/orgId。
 * 多租户、逻辑删除、审计由插件/处理器统一处理。</p>
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final AuthUserMapper userMapper;
    private final AuthUserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final CoreOrgMapper orgMapper;
    private final CoreStationMapper stationMapper;
    private final StpInterfaceImpl stpInterface;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /** 默认初始密码（新建用户未指定密码时） */
    private static final String DEFAULT_PASSWORD = "123456";

    public PageResult<UserVO> page(long pageNum, long pageSize, String keywords,
                                   Integer status, Long deptId) {
        IPage<AuthUser> result = userMapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<AuthUser>lambdaQuery()
                        .eq(status != null, AuthUser::getStatus, status)
                        .eq(deptId != null, AuthUser::getOrgId, deptId)
                        .and(StringUtils.hasText(keywords), w -> w
                                .like(AuthUser::getAccount, keywords)
                                .or().like(AuthUser::getName, keywords)
                                .or().like(AuthUser::getMobile, keywords))
                        .orderByDesc(AuthUser::getId));
        List<UserVO> list = enrich(result.getRecords());
        return new PageResult<>(list, result.getTotal());
    }

    /** 表单回显（含角色/机构/岗位） */
    public UserForm formData(Long id) {
        AuthUser u = mustGet(id);
        UserForm f = new UserForm();
        f.setId(String.valueOf(u.getId()));
        f.setUsername(u.getAccount());
        f.setNickname(u.getName());
        f.setAvatar(u.getAvatar());
        f.setGender(u.getSex());
        f.setMobile(u.getMobile());
        f.setEmail(u.getEmail());
        f.setStatus(u.getStatus());
        f.setDeptId(u.getOrgId() == null ? null : String.valueOf(u.getOrgId()));
        f.setStationId(u.getStationId() == null ? null : String.valueOf(u.getStationId()));
        f.setRoleIds(roleIdsOf(id));
        return f;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(UserForm form) {
        if (!StringUtils.hasText(form.getUsername())) {
            throw BizException.validFail("登录账号不能为空");
        }
        if (!StringUtils.hasText(form.getNickname())) {
            throw BizException.validFail("姓名不能为空");
        }
        Long tenantScoped = countByAccount(form.getUsername(), null);
        if (tenantScoped > 0) {
            throw BizException.validFail("登录账号已存在：" + form.getUsername());
        }
        AuthUser u = new AuthUser();
        u.setAccount(form.getUsername().trim());
        u.setName(form.getNickname().trim());
        u.setAvatar(form.getAvatar());
        u.setSex(form.getGender());
        u.setMobile(form.getMobile());
        u.setEmail(form.getEmail());
        u.setStatus(form.getStatus() == null ? 1 : form.getStatus());
        u.setOrgId(parseLong(form.getDeptId()));
        u.setStationId(parseLong(form.getStationId()));
        u.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        userMapper.insert(u);
        replaceRoles(u.getId(), form.getRoleIds());
        return u.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, UserForm form) {
        AuthUser exist = mustGet(id);
        if (StringUtils.hasText(form.getUsername()) && !form.getUsername().trim().equals(exist.getAccount())) {
            if (countByAccount(form.getUsername().trim(), id) > 0) {
                throw BizException.validFail("登录账号已存在：" + form.getUsername());
            }
            exist.setAccount(form.getUsername().trim());
        }
        if (StringUtils.hasText(form.getNickname())) {
            exist.setName(form.getNickname().trim());
        }
        exist.setAvatar(form.getAvatar());
        exist.setSex(form.getGender());
        exist.setMobile(form.getMobile());
        exist.setEmail(form.getEmail());
        if (form.getStatus() != null) {
            exist.setStatus(form.getStatus());
        }
        exist.setOrgId(parseLong(form.getDeptId()));
        exist.setStationId(parseLong(form.getStationId()));
        // 密码不在此修改；账号唯一性校验在上方
        userMapper.updateById(exist);
        if (form.getRoleIds() != null) {
            guardSuperAdmin(id, form.getStatus() == null ? exist.getStatus() : form.getStatus(),
                    form.getRoleIds());
            replaceRoles(id, form.getRoleIds());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long id, String newPassword) {
        mustGet(id);
        String pwd = StringUtils.hasText(newPassword) ? newPassword.trim() : DEFAULT_PASSWORD;
        if (pwd.length() < 6) {
            throw BizException.validFail("新密码长度不能少于6位");
        }
        AuthUser u = new AuthUser();
        u.setId(id);
        u.setPassword(passwordEncoder.encode(pwd));
        userMapper.updateById(u);
    }

    /** 启停 */
    public void changeStatus(Long id, Integer status) {
        AuthUser u = mustGet(id);
        if (status == null || (status != 0 && status != 1)) {
            throw BizException.validFail("状态值非法");
        }
        guardSuperAdmin(id, status, roleIdsOf(id));
        u.setStatus(status);
        userMapper.updateById(u);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteByIds(List<Long> ids) {
        for (Long id : ids) {
            if (stpInterface.isSuperAdmin(id)) {
                throw BizException.validFail("内置超级管理员账号不可删除");
            }
            userRoleMapper.delete(Wrappers.<AuthUserRole>lambdaQuery()
                    .eq(AuthUserRole::getUserId, id));
            userMapper.deleteById(id);
        }
    }

    // ============ 内部辅助 ============

    private AuthUser mustGet(Long id) {
        AuthUser u = userMapper.selectById(id);
        if (u == null) {
            throw BizException.validFail("用户不存在");
        }
        return u;
    }

    private Long countByAccount(String account, Long excludeId) {
        return userMapper.selectCount(Wrappers.<AuthUser>lambdaQuery()
                .eq(AuthUser::getAccount, account)
                .ne(excludeId != null, AuthUser::getId, excludeId));
    }

    private List<Long> roleIdsOf(Long userId) {
        return userRoleMapper.selectList(Wrappers.<AuthUserRole>lambdaQuery()
                        .eq(AuthUserRole::getUserId, userId)).stream()
                .map(AuthUserRole::getRoleId).distinct().toList();
    }

    /** 全量重写用户-角色绑定（uk_user_role 去重） */
    private void replaceRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.delete(Wrappers.<AuthUserRole>lambdaQuery()
                .eq(AuthUserRole::getUserId, userId));
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        // 仅保留存在且启用的角色，去重
        List<Role> roles = roleMapper.selectList(Wrappers.<Role>lambdaQuery()
                .in(Role::getId, roleIds).eq(Role::getStatus, 1));
        for (Role r : roles) {
            AuthUserRole link = new AuthUserRole();
            link.setUserId(userId);
            link.setRoleId(r.getId());
            // tenant_id 由租户插件在 insert 时填充（实体声明 tenantId、NOT NULL）
            userRoleMapper.insert(link);
        }
    }

    /** 禁止把超管停用或移除 ADMIN 角色（锁死最高权限防护） */
    private void guardSuperAdmin(Long userId, Integer newStatus, List<Long> newRoleIds) {
        if (stpInterface.isSuperAdmin(userId)) {
            if (newStatus != null && newStatus == 0) {
                throw BizException.validFail("内置超级管理员账号不可停用");
            }
            if (newRoleIds != null) {
                List<Role> roles = roleMapper.selectList(Wrappers.<Role>lambdaQuery()
                        .in(Role::getId, newRoleIds));
                boolean stillAdmin = roles.stream().anyMatch(r -> StpInterfaceImpl.SUPER_ROLE.equalsIgnoreCase(r.getCode()));
                if (!stillAdmin) {
                    throw BizException.validFail("不可移除超级管理员的 ADMIN 角色");
                }
            }
        }
    }

    private List<UserVO> enrich(List<AuthUser> users) {
        if (users.isEmpty()) {
            return new ArrayList<>();
        }
        // 机构/岗位名
        Set<Long> orgIds = users.stream().map(AuthUser::getOrgId).filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Set<Long> stationIds = users.stream().map(AuthUser::getStationId).filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> orgNames = orgIds.isEmpty() ? Collections.emptyMap()
                : orgMapper.selectBatchIds(orgIds).stream().collect(Collectors.toMap(CoreOrg::getId, CoreOrg::getName, (a, b) -> a));
        Map<Long, String> stationNames = stationIds.isEmpty() ? Collections.emptyMap()
                : stationMapper.selectBatchIds(stationIds).stream().collect(Collectors.toMap(CoreStation::getId, CoreStation::getName, (a, b) -> a));

        // 角色：一次查关联 + 一次查角色名，按 userId 分组
        Set<Long> userIds = users.stream().map(AuthUser::getId).collect(Collectors.toSet());
        Map<Long, List<Long>> roleIdMap = new HashMap<>();
        Map<Long, List<String>> roleNameMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<AuthUserRole> links = userRoleMapper.selectList(Wrappers.<AuthUserRole>lambdaQuery()
                    .in(AuthUserRole::getUserId, userIds));
            Set<Long> roleIds = links.stream().map(AuthUserRole::getRoleId).collect(Collectors.toSet());
            Map<Long, String> roleNameById = roleIds.isEmpty() ? Collections.emptyMap()
                    : roleMapper.selectBatchIds(roleIds).stream()
                    .collect(Collectors.toMap(Role::getId, Role::getName, (a, b) -> a));
            for (AuthUserRole l : links) {
                roleIdMap.computeIfAbsent(l.getUserId(), k -> new ArrayList<>()).add(l.getRoleId());
                String nm = roleNameById.get(l.getRoleId());
                if (nm != null) {
                    roleNameMap.computeIfAbsent(l.getUserId(), k -> new ArrayList<>()).add(nm);
                }
            }
        }
        return users.stream().map(u -> toVO(u, orgNames, stationNames,
                roleIdMap.get(u.getId()), roleNameMap.get(u.getId()))).collect(Collectors.toList());
    }

    private UserVO toVO(AuthUser u, Map<Long, String> orgNames, Map<Long, String> stationNames,
                        List<Long> roleIds, List<String> roleNames) {
        UserVO v = new UserVO();
        v.setId(String.valueOf(u.getId()));
        v.setUsername(u.getAccount());
        v.setNickname(u.getName());
        v.setAvatar(u.getAvatar());
        v.setGender(u.getSex());
        v.setMobile(u.getMobile());
        v.setEmail(u.getEmail());
        v.setStatus(u.getStatus());
        v.setDeptId(u.getOrgId() == null ? null : String.valueOf(u.getOrgId()));
        v.setDeptName(u.getOrgId() == null ? null : orgNames.get(u.getOrgId()));
        v.setStationId(u.getStationId() == null ? null : String.valueOf(u.getStationId()));
        v.setStationName(u.getStationId() == null ? null : stationNames.get(u.getStationId()));
        v.setRoleIds(roleIds);
        v.setRoleNames(roleNames == null ? null : String.join(",", roleNames));
        v.setCreateTime(u.getCreateTime());
        return v;
    }

    private Long parseLong(String s) {
        if (!StringUtils.hasText(s)) {
            return null;
        }
        try {
            return Long.valueOf(s.trim());
        } catch (NumberFormatException e) {
            throw BizException.validFail("ID 格式非法：" + s);
        }
    }
}
