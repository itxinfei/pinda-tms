package com.xinfei.auth.service.v1;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinfei.auth.common.BizException;
import com.xinfei.auth.common.v1.AppForm;
import com.xinfei.auth.common.v1.AppVO;
import com.xinfei.auth.common.v1.PageResult;
import com.xinfei.auth.entity.SysApp;
import com.xinfei.auth.mapper.SysAppMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 应用管理（youlai 契约 /api/v1/apps）：三方平台配置。
 */
@Service
@RequiredArgsConstructor
public class AppService {

    private final SysAppMapper appMapper;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public PageResult<AppVO> page(long pageNum, long pageSize, String keywords,
                                  Integer status, String platform) {
        IPage<SysApp> res = appMapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<SysApp>lambdaQuery()
                        .and(StringUtils.hasText(keywords), w -> w
                                .like(SysApp::getAppName, keywords)
                                .or().like(SysApp::getAppCode, keywords)
                                .or().like(SysApp::getAppId, keywords))
                        .eq(status != null, SysApp::getStatus, status)
                        .eq(StringUtils.hasText(platform), SysApp::getPlatform, platform)
                        .orderByDesc(SysApp::getId));
        return new PageResult<>(res.getRecords().stream().map(this::toVO).toList(), res.getTotal());
    }

    public AppForm form(Long id) {
        SysApp a = mustGet(id);
        AppForm f = new AppForm();
        f.setId(String.valueOf(a.getId()));
        f.setAppName(a.getAppName());
        f.setAppCode(a.getAppCode());
        f.setPlatform(a.getPlatform());
        f.setAppId(a.getAppId());
        f.setAppSecret(a.getAppSecret());
        f.setMerchantId(a.getMerchantId());
        f.setMerchantKey(a.getMerchantKey());
        f.setStatus(a.getStatus());
        f.setRemark(a.getRemark());
        f.setTenantId(a.getTenantId() == null ? null : String.valueOf(a.getTenantId()));
        return f;
    }

    @Transactional
    public Long create(AppForm form) {
        validate(form);
        if (appMapper.selectCount(Wrappers.<SysApp>lambdaQuery()
                .eq(SysApp::getAppCode, form.getAppCode().trim())) > 0) {
            throw BizException.validFail("应用编码已存在：" + form.getAppCode().trim());
        }
        SysApp a = new SysApp();
        apply(a, form);
        appMapper.insert(a);
        return a.getId();
    }

    @Transactional
    public void update(Long id, AppForm form) {
        SysApp a = mustGet(id);
        if (StringUtils.hasText(form.getAppCode()) && !form.getAppCode().trim().equals(a.getAppCode())) {
            if (appMapper.selectCount(Wrappers.<SysApp>lambdaQuery()
                    .eq(SysApp::getAppCode, form.getAppCode().trim())) > 0) {
                throw BizException.validFail("应用编码已存在：" + form.getAppCode().trim());
            }
        }
        apply(a, form);
        appMapper.updateById(a);
    }

    @Transactional
    public void deleteByIds(List<Long> ids) {
        appMapper.deleteByIds(ids);
    }

    public void updateStatus(Long id, Integer status) {
        SysApp a = mustGet(id);
        a.setStatus(status);
        appMapper.updateById(a);
    }

    private void apply(SysApp a, AppForm form) {
        if (StringUtils.hasText(form.getAppName())) {
            a.setAppName(form.getAppName().trim());
        }
        if (StringUtils.hasText(form.getAppCode())) {
            a.setAppCode(form.getAppCode().trim());
        }
        if (StringUtils.hasText(form.getPlatform())) {
            a.setPlatform(form.getPlatform().trim());
        }
        if (StringUtils.hasText(form.getAppId())) {
            a.setAppId(form.getAppId().trim());
        }
        a.setAppSecret(form.getAppSecret());
        a.setMerchantId(form.getMerchantId());
        a.setMerchantKey(form.getMerchantKey());
        if (form.getStatus() != null) {
            a.setStatus(form.getStatus());
        } else if (a.getStatus() == null) {
            a.setStatus(1);
        }
        a.setRemark(form.getRemark());
        a.setTenantId(parseTenant(form.getTenantId()));
    }

    private Long parseTenant(String s) {
        if (!StringUtils.hasText(s)) {
            return 0L;
        }
        try {
            return Long.parseLong(s.trim());
        } catch (Exception e) {
            return 0L;
        }
    }

    private void validate(AppForm form) {
        if (!StringUtils.hasText(form.getAppName())
                || !StringUtils.hasText(form.getAppCode())
                || !StringUtils.hasText(form.getPlatform())
                || !StringUtils.hasText(form.getAppId())) {
            throw BizException.validFail("应用名称、编码、平台、AppID 不能为空");
        }
    }

    private SysApp mustGet(Long id) {
        SysApp a = appMapper.selectById(id);
        if (a == null) {
            throw BizException.validFail("应用不存在");
        }
        return a;
    }

    private AppVO toVO(SysApp a) {
        AppVO v = new AppVO();
        v.setId(String.valueOf(a.getId()));
        v.setAppName(a.getAppName());
        v.setAppCode(a.getAppCode());
        v.setPlatform(a.getPlatform());
        v.setAppId(a.getAppId());
        v.setAppSecret(a.getAppSecret());
        v.setMerchantId(a.getMerchantId());
        v.setMerchantKey(a.getMerchantKey());
        v.setStatus(a.getStatus());
        v.setRemark(a.getRemark());
        v.setTenantId(a.getTenantId() == null ? null : String.valueOf(a.getTenantId()));
        v.setCreateTime(fmt(a.getCreateTime()));
        v.setUpdateTime(fmt(a.getUpdateTime()));
        return v;
    }

    private String fmt(LocalDateTime t) {
        return t == null ? null : t.format(FMT);
    }
}