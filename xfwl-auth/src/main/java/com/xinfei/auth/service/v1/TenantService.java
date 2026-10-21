package com.xinfei.auth.service.v1;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinfei.auth.common.BizException;
import com.xinfei.auth.common.v1.PageResult;
import com.xinfei.auth.common.v1.TenantCreateForm;
import com.xinfei.auth.common.v1.TenantCreateResult;
import com.xinfei.auth.common.v1.TenantForm;
import com.xinfei.auth.common.v1.TenantInfo;
import com.xinfei.auth.common.v1.TenantVO;
import com.xinfei.auth.entity.AuthTenant;
import com.xinfei.auth.entity.AuthUser;
import com.xinfei.auth.entity.AuthUserRole;
import com.xinfei.auth.entity.SysTenantMenu;
import com.xinfei.auth.mapper.AuthTenantMapper;
import com.xinfei.auth.mapper.AuthUserMapper;
import com.xinfei.auth.mapper.AuthUserRoleMapper;
import com.xinfei.auth.mapper.TenantMenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 平台租户管理（youlai 契约 /api/v1/tenants）。
 *
 * <p>sys_tenant 为平台级表（租户插件忽略）。新增租户时一并初始化管理员账号并绑定内置 ADMIN 角色。</p>
 */
@Service
@RequiredArgsConstructor
public class TenantService {

    private final AuthTenantMapper tenantMapper;
    private final TenantMenuMapper tenantMenuMapper;
    private final AuthUserMapper userMapper;
    private final AuthUserRoleMapper userRoleMapper;

    private static final BCryptPasswordEncoder ENC = new BCryptPasswordEncoder();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public PageResult<TenantVO> page(long pageNum, long pageSize, String keywords, Integer status) {
        IPage<AuthTenant> res = tenantMapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<AuthTenant>lambdaQuery()
                        .and(StringUtils.hasText(keywords), w -> w
                                .like(AuthTenant::getName, keywords)
                                .or().like(AuthTenant::getCode, keywords)
                                .or().like(AuthTenant::getDomain, keywords))
                        .eq(status != null, AuthTenant::getStatus, status)
                        .orderByDesc(AuthTenant::getId));
        List<TenantVO> list = res.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(list, res.getTotal());
    }

    /** 当前登录用户可访问的租户列表（启用态） */
    public List<TenantInfo> options() {
        return tenantMapper.selectList(Wrappers.<AuthTenant>lambdaQuery()
                        .eq(AuthTenant::getStatus, 1).orderByAsc(AuthTenant::getId))
                .stream().map(this::toInfo).toList();
    }

    /** 当前租户信息（按登录用户所属租户） */
    public TenantInfo current() {
        Long tenantId = currentTenantId();
        if (tenantId == null) {
            return null;
        }
        AuthTenant t = tenantMapper.selectById(tenantId);
        return t == null ? null : toInfo(t);
    }

    /** 切换租户（返回目标租户；前端自行持久化当前租户） */
    public TenantInfo switchTenant(Long tenantId) {
        AuthTenant t = tenantMapper.selectById(tenantId);
        if (t == null) {
            throw BizException.validFail("租户不存在");
        }
        return toInfo(t);
    }

    public TenantForm form(Long id) {
        AuthTenant t = mustGet(id);
        TenantForm f = new TenantForm();
        f.setId(String.valueOf(t.getId()));
        f.setName(t.getName());
        f.setCode(t.getCode());
        f.setContactName(t.getContactName());
        f.setContactEmail(t.getContactEmail());
        f.setContactPhone(t.getContactPhone());
        f.setDomain(t.getDomain());
        f.setLogo(t.getLogo());
        f.setPlanId(t.getPlanId());
        f.setStatus(t.getStatus());
        f.setRemark(t.getRemark());
        f.setExpireTime(fmt(t.getExpireTime()));
        return f;
    }

    @Transactional
    public TenantCreateResult create(TenantCreateForm form) {
        if (!StringUtils.hasText(form.getName()) || !StringUtils.hasText(form.getCode())) {
            throw BizException.validFail("租户名称与编码不能为空");
        }
        String code = form.getCode().trim();
        if (tenantMapper.selectCount(Wrappers.<AuthTenant>lambdaQuery().eq(AuthTenant::getCode, code)) > 0) {
            throw BizException.validFail("租户编码已存在：" + code);
        }
        AuthTenant t = new AuthTenant();
        t.setCode(code);
        t.setName(form.getName().trim());
        t.setContactName(form.getContactName());
        t.setContactEmail(form.getContactEmail());
        t.setContactPhone(form.getContactPhone());
        t.setDomain(form.getDomain());
        t.setLogo(form.getLogo());
        t.setPlanId(form.getPlanId());
        t.setStatus(form.getStatus() == null ? 1 : form.getStatus());
        t.setRemark(form.getRemark());
        t.setExpireTime(parse(form.getExpireTime()));
        tenantMapper.insert(t);
        Long tenantId = t.getId();

        // 初始化管理员账号
        String admin = (form.getAdminUsername() != null && !form.getAdminUsername().isBlank())
                ? form.getAdminUsername().trim() : "admin_" + code;
        String pwd = randomPassword();
        AuthUser u = new AuthUser();
        u.setTenantId(tenantId);
        u.setAccount(admin);
        u.setName(admin);
        u.setPassword(ENC.encode(pwd));
        u.setStatus(1);
        u.setMobile("");
        u.setSex(0);
        userMapper.insert(u);
        AuthUserRole ur = new AuthUserRole();
        ur.setTenantId(tenantId);
        ur.setUserId(u.getId());
        ur.setRoleId(1L);
        userRoleMapper.insert(ur);

        TenantCreateResult r = new TenantCreateResult();
        r.setTenantId(String.valueOf(tenantId));
        r.setTenantCode(t.getCode());
        r.setTenantName(t.getName());
        r.setAdminUsername(admin);
        r.setAdminInitialPassword(pwd);
        r.setAdminRoleCode("ADMIN");
        return r;
    }

    @Transactional
    public void update(Long id, TenantForm form) {
        AuthTenant t = mustGet(id);
        if (StringUtils.hasText(form.getCode()) && !form.getCode().trim().equals(t.getCode())) {
            if (tenantMapper.selectCount(Wrappers.<AuthTenant>lambdaQuery()
                    .eq(AuthTenant::getCode, form.getCode().trim())) > 0) {
                throw BizException.validFail("租户编码已存在：" + form.getCode().trim());
            }
            t.setCode(form.getCode().trim());
        }
        if (StringUtils.hasText(form.getName())) {
            t.setName(form.getName().trim());
        }
        t.setContactName(form.getContactName());
        t.setContactEmail(form.getContactEmail());
        t.setContactPhone(form.getContactPhone());
        t.setDomain(form.getDomain());
        t.setLogo(form.getLogo());
        t.setPlanId(form.getPlanId());
        t.setStatus(form.getStatus());
        t.setRemark(form.getRemark());
        t.setExpireTime(parse(form.getExpireTime()));
        tenantMapper.updateById(t);
    }

    @Transactional
    public void deleteByIds(List<Long> ids) {
        tenantMapper.deleteByIds(ids);
        ids.forEach(tid -> tenantMenuMapper.delete(
                Wrappers.<SysTenantMenu>lambdaQuery().eq(SysTenantMenu::getTenantId, tid)));
    }

    public void updateStatus(Long id, Integer status) {
        AuthTenant t = mustGet(id);
        t.setStatus(status);
        tenantMapper.updateById(t);
    }

    public List<Long> getMenuIds(Long tenantId) {
        return tenantMenuMapper.selectList(Wrappers.<SysTenantMenu>lambdaQuery()
                        .eq(SysTenantMenu::getTenantId, tenantId))
                .stream().map(SysTenantMenu::getMenuId).toList();
    }

    @Transactional
    public void updateMenus(Long tenantId, List<Long> menuIds) {
        tenantMenuMapper.delete(Wrappers.<SysTenantMenu>lambdaQuery()
                .eq(SysTenantMenu::getTenantId, tenantId));
        if (menuIds != null) {
            for (Long mid : menuIds) {
                SysTenantMenu tm = new SysTenantMenu();
                tm.setTenantId(tenantId);
                tm.setMenuId(mid);
                tenantMenuMapper.insert(tm);
            }
        }
    }

    private Long currentTenantId() {
        try {
            Long userId = StpUtil.getLoginIdAsLong();
            AuthUser u = userMapper.selectById(userId);
            return u == null ? null : u.getTenantId();
        } catch (Exception e) {
            return null;
        }
    }

    private AuthTenant mustGet(Long id) {
        AuthTenant t = tenantMapper.selectById(id);
        if (t == null) {
            throw BizException.validFail("租户不存在");
        }
        return t;
    }

    private TenantInfo toInfo(AuthTenant t) {
        TenantInfo i = new TenantInfo();
        i.setId(t.getId());
        i.setName(t.getName());
        i.setDomain(t.getDomain());
        return i;
    }

    private TenantVO toVO(AuthTenant t) {
        TenantVO v = new TenantVO();
        v.setId(String.valueOf(t.getId()));
        v.setName(t.getName());
        v.setCode(t.getCode());
        v.setContactName(t.getContactName());
        v.setContactEmail(t.getContactEmail());
        v.setContactPhone(t.getContactPhone());
        v.setDomain(t.getDomain());
        v.setLogo(t.getLogo());
        v.setPlanId(t.getPlanId());
        v.setStatus(t.getStatus());
        v.setRemark(t.getRemark());
        v.setExpireTime(fmt(t.getExpireTime()));
        v.setCreateTime(fmt(t.getCreateTime()));
        v.setUpdateTime(fmt(t.getUpdateTime()));
        return v;
    }

    private String fmt(LocalDateTime t) {
        return t == null ? null : t.format(FMT);
    }

    private LocalDateTime parse(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        String v = s.trim().replace('T', ' ');
        try {
            if (v.length() > 19) {
                v = v.substring(0, 19);
            }
            return LocalDateTime.parse(v, FMT);
        } catch (Exception e) {
            return null;
        }
    }

    private String randomPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
        Random r = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            sb.append(chars.charAt(r.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
