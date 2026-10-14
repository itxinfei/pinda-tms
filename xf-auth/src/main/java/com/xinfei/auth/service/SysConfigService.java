package com.xinfei.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinfei.auth.common.BizException;
import com.xinfei.auth.entity.SysConfig;
import com.xinfei.auth.mapper.SysConfigMapper;
import com.xinfei.common.config.dynamic.ConfigCrypto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 参数配置管理（CFG-01）：分页 / 详情 / 新增 / 修改 / 删除。
 *
 * <p>平台全局表（IGNORE_TABLES 放行租户插件），所有数据固定 {@code tenant_id=0}，此处再显式兜底。
 * 敏感值写入前 AES 加密；编辑留空表示不修改原值；config_key 租户内唯一。
 * 内部链路无登录态，审计 createBy/updateBy 置 null。</p>
 */
@Service
@RequiredArgsConstructor
public class SysConfigService {

    /** 平台全局租户 ID */
    private static final long PLATFORM_TENANT_ID = 0L;

    private final SysConfigMapper configMapper;
    private final ConfigCrypto configCrypto;

    /**
     * 分页：按名称/键名关键字模糊、状态精确过滤，按键名升序。
     */
    public IPage<SysConfig> page(long pageNo, long size, String keywords, Integer status) {
        LambdaQueryWrapper<SysConfig> query = Wrappers.<SysConfig>lambdaQuery()
                .eq(SysConfig::getTenantId, PLATFORM_TENANT_ID)
                .and(StringUtils.hasText(keywords), w -> w
                        .like(SysConfig::getConfigName, keywords)
                        .or().like(SysConfig::getConfigKey, keywords))
                .eq(status != null, SysConfig::getStatus, status)
                .orderByAsc(SysConfig::getConfigKey);
        return configMapper.selectPage(new Page<>(pageNo, size), query);
    }

    /**
     * 详情（实体，configValue 为密文，脱敏由 controller 负责）。
     */
    public SysConfig detail(Long id) {
        SysConfig config = configMapper.selectById(id);
        if (config == null) {
            throw new BizException("参数配置不存在");
        }
        return config;
    }

    /**
     * 新增，返回新 ID。
     */
    public Long create(SysConfig entity) {
        validateKey(entity.getConfigKey());
        if (countByKey(entity.getConfigKey(), null) > 0) {
            throw new BizException("参数键名已存在：" + entity.getConfigKey());
        }

        entity.setId(null);
        entity.setTenantId(PLATFORM_TENANT_ID);
        entity.setConfigKey(entity.getConfigKey().trim());
        entity.setValueType(normalizeValueType(entity.getValueType()));
        entity.setIsSensitive(entity.getIsSensitive() != null && entity.getIsSensitive() == 1 ? 1 : 0);
        entity.setStatus(entity.getStatus() != null && entity.getStatus() == 0 ? 0 : 1);
        entity.setConfigValue(encryptIfSensitive(entity.getIsSensitive(), entity.getConfigValue()));
        entity.setCreateBy(null);
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateBy(null);
        entity.setUpdateTime(null);
        configMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 修改。configValue 留空=不修改；敏感性不允许变更（避免明文/密文语义错乱）。
     */
    public void update(SysConfig entity) {
        if (entity.getId() == null) {
            throw new BizException("缺少参数配置ID");
        }
        SysConfig existing = configMapper.selectById(entity.getId());
        if (existing == null) {
            throw new BizException("参数配置不存在");
        }

        validateKey(entity.getConfigKey());
        String newKey = entity.getConfigKey().trim();
        if (!newKey.equals(existing.getConfigKey()) && countByKey(newKey, entity.getId()) > 0) {
            throw new BizException("参数键名已存在：" + newKey);
        }

        existing.setConfigName(entity.getConfigName());
        existing.setConfigKey(newKey);
        existing.setValueType(normalizeValueType(entity.getValueType()));
        existing.setStatus(entity.getStatus() != null && entity.getStatus() == 0 ? 0 : 1);
        existing.setRemark(entity.getRemark());

        // 留空不改；有值时按既有敏感性加密/明文落库
        if (StringUtils.hasText(entity.getConfigValue())) {
            existing.setConfigValue(encryptIfSensitive(existing.getIsSensitive(), entity.getConfigValue()));
        }

        existing.setUpdateBy(null);
        existing.setUpdateTime(LocalDateTime.now());
        configMapper.updateById(existing);
    }

    /**
     * 物理删除。
     */
    public void remove(Long id) {
        if (configMapper.selectById(id) == null) {
            throw new BizException("参数配置不存在");
        }
        configMapper.deleteById(id);
    }

    /**
     * 敏感值加密：非敏感或空值原样落库。
     */
    private String encryptIfSensitive(Integer isSensitive, String value) {
        if (isSensitive != null && isSensitive == 1 && StringUtils.hasText(value)) {
            return configCrypto.encrypt(value.trim());
        }
        return value == null ? "" : value;
    }

    private long countByKey(String configKey, Long excludeId) {
        return configMapper.selectCount(Wrappers.<SysConfig>lambdaQuery()
                .eq(SysConfig::getTenantId, PLATFORM_TENANT_ID)
                .eq(SysConfig::getConfigKey, configKey)
                .ne(excludeId != null, SysConfig::getId, excludeId));
    }

    private void validateKey(String configKey) {
        if (!StringUtils.hasText(configKey)) {
            throw new BizException("参数键名不能为空");
        }
        if (!configKey.trim().matches("[A-Za-z0-9_.-]+")) {
            throw new BizException("参数键名只能包含字母、数字、点、下划线、中划线");
        }
    }

    private String normalizeValueType(String valueType) {
        return StringUtils.hasText(valueType) ? valueType.trim() : "string";
    }
}
