package com.itheima.auth.service.v1;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.auth.common.BizException;
import com.itheima.auth.common.v1.OptionVO;
import com.itheima.auth.common.v1.PageResult;
import com.itheima.auth.common.v1.RoleForm;
import com.itheima.auth.common.v1.RoleVO;
import com.itheima.auth.config.StpInterfaceImpl;
import com.itheima.auth.entity.AuthUserRole;
import com.itheima.auth.entity.Role;
import com.itheima.auth.entity.RoleMenu;
import com.itheima.auth.mapper.AuthUserRoleMapper;
import com.itheima.auth.mapper.RoleMapper;
import com.itheima.auth.mapper.RoleMenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 管理端角色管理（youlai 契约，sys_role / sys_role_menu）。
 * 内置角色(readonly=1)/ADMIN 不可删除、不可停用、不可改 code。
 * 数据权限(dataScope/deptIds)为 P2 预留，本期忽略、固定"全部数据"。
 */
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleMapper roleMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final AuthUserRoleMapper userRoleMapper;

    public PageResult<RoleVO> page(long pageNum, long pageSize, String keywords) {
        IPage<Role> result = roleMapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<Role>lambdaQuery()
                        .and(StringUtils.hasText(keywords), w -> w
                                .like(Role::getName, keywords)
                                .or().like(Role::getCode, keywords))
                        .orderByAsc(Role::getId));
        List<RoleVO> list = result.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(list, result.getTotal());
    }

    public RoleForm formData(Long id) {
        Role r = mustGet(id);
        RoleForm f = new RoleForm();
        f.setId(String.valueOf(r.getId()));
        f.setCode(r.getCode());
        f.setName(r.getName());
        f.setSort(1);
        f.setStatus(r.getStatus());
        f.setDataScope(1);
        f.setRemark(r.getDescription());
        return f;
    }

    public List<OptionVO> options() {
        return roleMapper.selectList(Wrappers.<Role>lambdaQuery()
                        .eq(Role::getStatus, 1).orderByAsc(Role::getId))
                .stream().map(r -> {
                    OptionVO o = new OptionVO();
                    o.setValue(String.valueOf(r.getId()));
                    o.setLabel(r.getName());
                    return o;
                }).toList();
    }

    /** 角色编码选项（value=code） */
    public List<OptionVO> codeOptions() {
        return roleMapper.selectList(Wrappers.<Role>lambdaQuery()
                        .eq(Role::getStatus, 1).orderByAsc(Role::getId))
                .stream().map(r -> {
                    OptionVO o = new OptionVO();
                    o.setValue(r.getCode());
                    o.setLabel(r.getName());
                    return o;
                }).toList();
    }

    public Long create(RoleForm form) {
        validate(form);
        if (countByCode(form.getCode(), null) > 0) {
            throw BizException.validFail("角色编码已存在：" + form.getCode());
        }
        Role r = new Role();
        r.setCode(form.getCode().trim());
        r.setName(form.getName().trim());
        r.setDescription(form.getRemark());
        r.setStatus(form.getStatus() == null ? 1 : form.getStatus());
        r.setReadonly(0);
        roleMapper.insert(r);
        return r.getId();
    }

    public void update(Long id, RoleForm form) {
        Role exist = mustGet(id);
        validate(form);
        if (isBuiltin(exist)) {
            // 内置角色：禁止改 code、停用；名称/描述可改
            if (form.getStatus() != null && form.getStatus() == 0) {
                throw BizException.validFail("内置角色不可停用");
            }
        }
        if (StringUtils.hasText(form.getCode()) && !form.getCode().trim().equals(exist.getCode())) {
            if (isBuiltin(exist)) {
                throw BizException.validFail("内置角色编码不可修改");
            }
            if (countByCode(form.getCode().trim(), id) > 0) {
                throw BizException.validFail("角色编码已存在：" + form.getCode());
            }
            exist.setCode(form.getCode().trim());
        }
        if (StringUtils.hasText(form.getName())) {
            exist.setName(form.getName().trim());
        }
        exist.setDescription(form.getRemark());
        if (form.getStatus() != null && !isBuiltin(exist)) {
            exist.setStatus(form.getStatus());
        }
        roleMapper.updateById(exist);
    }

    /** 角色已分配的菜单/按钮 id */
    public List<Long> menuIds(Long roleId) {
        mustGet(roleId);
        return roleMenuMapper.selectMenuIdsByRoleIds(List.of(roleId));
    }

    /** 全量重写角色-菜单授权 */
    @Transactional(rollbackFor = Exception.class)
    public void updateMenus(Long roleId, List<Long> menuIds) {
        mustGet(roleId);
        roleMenuMapper.delete(Wrappers.<RoleMenu>lambdaQuery().eq(RoleMenu::getRoleId, roleId));
        if (menuIds == null || menuIds.isEmpty()) {
            return;
        }
        for (Long menuId : new ArrayList<>(menuIds).stream().distinct().toList()) {
            RoleMenu rm = new RoleMenu();
            rm.setRoleId(roleId);
            rm.setMenuId(menuId);
            // tenant_id 由租户插件 insert 时填充
            roleMenuMapper.insert(rm);
        }
    }

    /** 角色已分配的部门数据权限 id：P2 数据权限，本期固定空（=全部数据） */
    public List<Long> deptIds(Long roleId) {
        mustGet(roleId);
        return List.of();
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteByIds(List<Long> ids) {
        for (Long id : ids) {
            Role r = mustGet(id);
            if (isBuiltin(r) || StpInterfaceImpl.SUPER_ROLE.equalsIgnoreCase(r.getCode())) {
                throw BizException.validFail("内置角色[" + r.getName() + "]不可删除");
            }
            Long userCount = userRoleMapper.selectCount(Wrappers.<AuthUserRole>lambdaQuery()
                    .eq(AuthUserRole::getRoleId, id));
            if (userCount != null && userCount > 0) {
                throw BizException.validFail("角色[" + r.getName() + "]已分配给用户，请先解除关联");
            }
            roleMenuMapper.delete(Wrappers.<RoleMenu>lambdaQuery().eq(RoleMenu::getRoleId, id));
            roleMapper.deleteById(id);
        }
    }

    private Role mustGet(Long id) {
        Role r = roleMapper.selectById(id);
        if (r == null) {
            throw BizException.validFail("角色不存在");
        }
        return r;
    }

    private boolean isBuiltin(Role r) {
        return r.getReadonly() != null && r.getReadonly() == 1;
    }

    private Long countByCode(String code, Long excludeId) {
        return roleMapper.selectCount(Wrappers.<Role>lambdaQuery()
                .eq(Role::getCode, code)
                .ne(excludeId != null, Role::getId, excludeId));
    }

    private void validate(RoleForm form) {
        if (!StringUtils.hasText(form.getName())) {
            throw BizException.validFail("角色名称不能为空");
        }
        if (!StringUtils.hasText(form.getCode())) {
            throw BizException.validFail("角色编码不能为空");
        }
    }

    private RoleVO toVO(Role r) {
        RoleVO v = new RoleVO();
        v.setId(String.valueOf(r.getId()));
        v.setCode(r.getCode());
        v.setName(r.getName());
        v.setSort(1);
        v.setStatus(r.getStatus());
        v.setRemark(r.getDescription());
        v.setUpdateTime(r.getUpdateTime());
        return v;
    }
}
