package com.xinfei.auth.service.v1;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinfei.auth.common.BizException;
import com.xinfei.auth.common.v1.OptionVO;
import com.xinfei.auth.common.v1.PageResult;
import com.xinfei.auth.common.v1.TenantPlanForm;
import com.xinfei.auth.common.v1.TenantPlanVO;
import com.xinfei.auth.entity.AuthTenant;
import com.xinfei.auth.entity.SysTenantPlan;
import com.xinfei.auth.entity.SysTenantPlanMenu;
import com.xinfei.auth.mapper.AuthTenantMapper;
import com.xinfei.auth.mapper.SysTenantPlanMapper;
import com.xinfei.auth.mapper.SysTenantPlanMenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 租户套餐管理（youlai 契约 /api/v1/tenant-plans）。
 *
 * <p>套餐与菜单为多对多（sys_tenant_plan_menu）；租户通过 sys_tenant.plan_id 关联套餐。
 * 删除套餐时校验是否仍被租户引用，被引用则拒绝（占用校验，与"有下级拒绝"同范式）。</p>
 */
@Service
@RequiredArgsConstructor
public class TenantPlanService {

    private final SysTenantPlanMapper planMapper;
    private final SysTenantPlanMenuMapper planMenuMapper;
    private final AuthTenantMapper tenantMapper;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public PageResult<TenantPlanVO> page(long pageNum, long pageSize, String keywords, Integer status) {
        IPage<SysTenantPlan> res = planMapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<SysTenantPlan>lambdaQuery()
                        .and(StringUtils.hasText(keywords), w -> w
                                .like(SysTenantPlan::getName, keywords)
                                .or().like(SysTenantPlan::getCode, keywords))
                        .eq(status != null, SysTenantPlan::getStatus, status)
                        .orderByAsc(SysTenantPlan::getSort).orderByAsc(SysTenantPlan::getId));
        return new PageResult<>(res.getRecords().stream().map(this::toVO).toList(), res.getTotal());
    }

    public List<OptionVO> options() {
        return planMapper.selectList(Wrappers.<SysTenantPlan>lambdaQuery()
                        .eq(SysTenantPlan::getStatus, 1).orderByAsc(SysTenantPlan::getSort))
                .stream().map(p -> {
                    OptionVO o = new OptionVO();
                    o.setValue(String.valueOf(p.getId()));
                    o.setLabel(p.getName());
                    return o;
                }).toList();
    }

    public TenantPlanForm form(Long id) {
        SysTenantPlan p = mustGet(id);
        TenantPlanForm f = new TenantPlanForm();
        f.setId(p.getId());
        f.setName(p.getName());
        f.setCode(p.getCode());
        f.setStatus(p.getStatus());
        f.setSort(p.getSort());
        f.setRemark(p.getRemark());
        return f;
    }

    @Transactional
    public Long create(TenantPlanForm form) {
        validate(form);
        if (planMapper.selectCount(Wrappers.<SysTenantPlan>lambdaQuery()
                .eq(SysTenantPlan::getCode, form.getCode().trim())) > 0) {
            throw BizException.validFail("套餐编码已存在：" + form.getCode().trim());
        }
        SysTenantPlan p = new SysTenantPlan();
        p.setName(form.getName().trim());
        p.setCode(form.getCode().trim());
        p.setStatus(form.getStatus() == null ? 1 : form.getStatus());
        p.setSort(form.getSort() == null ? 1 : form.getSort());
        p.setRemark(form.getRemark());
        planMapper.insert(p);
        return p.getId();
    }

    @Transactional
    public void update(Long id, TenantPlanForm form) {
        SysTenantPlan p = mustGet(id);
        if (StringUtils.hasText(form.getCode()) && !form.getCode().trim().equals(p.getCode())) {
            if (planMapper.selectCount(Wrappers.<SysTenantPlan>lambdaQuery()
                    .eq(SysTenantPlan::getCode, form.getCode().trim())) > 0) {
                throw BizException.validFail("套餐编码已存在：" + form.getCode().trim());
            }
            p.setCode(form.getCode().trim());
        }
        if (StringUtils.hasText(form.getName())) {
            p.setName(form.getName().trim());
        }
        if (form.getStatus() != null) {
            p.setStatus(form.getStatus());
        }
        if (form.getSort() != null) {
            p.setSort(form.getSort());
        }
        p.setRemark(form.getRemark());
        planMapper.updateById(p);
    }

    @Transactional
    public void deleteByIds(List<Long> ids) {
        // 占用校验：仍被租户引用的套餐不允许删除
        for (Long id : ids) {
            Long used = tenantMapper.selectCount(Wrappers.<AuthTenant>lambdaQuery()
                    .eq(AuthTenant::getPlanId, id));
            if (used != null && used > 0) {
                throw BizException.validFail("套餐已被 " + used + " 个租户引用，无法删除");
            }
        }
        planMapper.deleteByIds(ids);
        planMenuMapper.delete(Wrappers.<SysTenantPlanMenu>lambdaQuery()
                .in(SysTenantPlanMenu::getPlanId, ids));
    }

    public List<Long> getMenuIds(Long planId) {
        return planMenuMapper.selectList(Wrappers.<SysTenantPlanMenu>lambdaQuery()
                        .eq(SysTenantPlanMenu::getPlanId, planId))
                .stream().map(SysTenantPlanMenu::getMenuId).toList();
    }

    @Transactional
    public void updateMenus(Long planId, List<Long> menuIds) {
        mustGet(planId);
        planMenuMapper.delete(Wrappers.<SysTenantPlanMenu>lambdaQuery()
                .eq(SysTenantPlanMenu::getPlanId, planId));
        if (menuIds != null) {
            for (Long menuId : menuIds) {
                SysTenantPlanMenu pm = new SysTenantPlanMenu();
                pm.setPlanId(planId);
                pm.setMenuId(menuId);
                planMenuMapper.insert(pm);
            }
        }
    }

    private void validate(TenantPlanForm form) {
        if (!StringUtils.hasText(form.getName()) || !StringUtils.hasText(form.getCode())) {
            throw BizException.validFail("套餐名称与编码不能为空");
        }
    }

    private SysTenantPlan mustGet(Long id) {
        SysTenantPlan p = planMapper.selectById(id);
        if (p == null) {
            throw BizException.validFail("租户套餐不存在");
        }
        return p;
    }

    private TenantPlanVO toVO(SysTenantPlan p) {
        TenantPlanVO v = new TenantPlanVO();
        v.setId(p.getId());
        v.setName(p.getName());
        v.setCode(p.getCode());
        v.setStatus(p.getStatus());
        v.setSort(p.getSort());
        v.setRemark(p.getRemark());
        v.setCreateTime(fmt(p.getCreateTime()));
        v.setUpdateTime(fmt(p.getUpdateTime()));
        return v;
    }

    private String fmt(LocalDateTime t) {
        return t == null ? null : t.format(FMT);
    }
}