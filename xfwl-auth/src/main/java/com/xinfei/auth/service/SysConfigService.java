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
     * 按 key 取「平台全局、启用中」的配置值（已解密；无启用项或值为空返回 null）。
     * 供内部 Feign 端点（/internal/config/key/{key}）服务无数据源的消费方（如 BFF）。
     */
    public String findEnabledValueByKey(String configKey) {
        if (!StringUtils.hasText(configKey)) {
            return null;
        }
        SysConfig config = configMapper.selectOne(Wrappers.<SysConfig>lambdaQuery()
                .eq(SysConfig::getTenantId, PLATFORM_TENANT_ID)
                .eq(SysConfig::getConfigKey, configKey.trim())
                .eq(SysConfig::getStatus, 1)
                .last("LIMIT 1"));
        if (config == null || !StringUtils.hasText(config.getConfigValue())) {
            return null;
        }
        return config.getIsSensitive() != null && config.getIsSensitive() == 1
                ? configCrypto.decrypt(config.getConfigValue())
                : config.getConfigValue();
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
     * 修改。configValue 留空=不修改值本身。
     * <p>isSensitive 允许变更：切换时把存量值按新设置重新加密/解密落库，避免明文/密文语义错乱；
     * 同时传入 configValue 时不做存量转换，直接按新敏感性加密/明文落库。</p>
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

        int newSensitive = entity.getIsSensitive() != null && entity.getIsSensitive() == 1 ? 1 : 0;
        boolean sensitiveChanged = newSensitive != (existing.getIsSensitive() != null ? existing.getIsSensitive() : 0);

        if (StringUtils.hasText(entity.getConfigValue())) {
            // 显式传值：直接按新敏感性落库
            existing.setConfigValue(encryptIfSensitive(newSensitive, entity.getConfigValue()));
        } else if (sensitiveChanged) {
            // 只切敏感性不改值：先把存量值还原为明文，再按新设置落库
            String plain = existing.getIsSensitive() != null && existing.getIsSensitive() == 1
                    ? decryptStored(existing.getConfigValue())
                    : existing.getConfigValue();
            existing.setConfigValue(encryptIfSensitive(newSensitive, plain));
        }
        existing.setIsSensitive(newSensitive);

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

    /**
     * 存量密文还原为明文（敏感性切换时用）；解密失败按业务异常抛出，避免把坏密文降级为明文。
     */
    private String decryptStored(String stored) {
        try {
            return configCrypto.decrypt(stored);
        } catch (Exception e) {
            throw new BizException("存量值解密失败，无法切换敏感性（请直接重新输入参数值后保存）");
        }
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
