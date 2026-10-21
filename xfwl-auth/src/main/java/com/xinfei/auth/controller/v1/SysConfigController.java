package com.xinfei.auth.controller.v1;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xinfei.auth.common.v1.ApiResult;
import com.xinfei.auth.common.v1.ConfigForm;
import com.xinfei.auth.common.v1.ConfigVO;
import com.xinfei.auth.common.v1.PageResult;
import com.xinfei.auth.entity.SysConfig;
import com.xinfei.auth.service.SysConfigService;
import com.xinfei.common.config.dynamic.ConfigCrypto;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 参数配置管理（CFG-01，youlai 契约 /api/v1/sys-config）。
 *
 * <p>平台全局配置（tenant_id=0），敏感值 AES 加密存储、列表/详情脱敏回显。
 * 权限码与 sys_menu 26601-26605 注册一致（sys:config:*）。</p>
 */
@RestController
@RequestMapping("/sys-config")
@RequiredArgsConstructor
public class SysConfigController {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SysConfigService configService;
    private final ConfigCrypto configCrypto;

    @GetMapping("/page")
    @SaCheckPermission("sys:config:list")
    public ApiResult<PageResult<ConfigVO>> page(
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize,
            @RequestParam(value = "keywords", required = false) String keywords,
            @RequestParam(value = "status", required = false) Integer status) {
        IPage<SysConfig> result = configService.page(pageNum, pageSize, keywords, status);
        List<ConfigVO> list = result.getRecords().stream().map(this::toVO).toList();
        return ApiResult.success(PageResult.of(list, result.getTotal()));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("sys:config:detail")
    public ApiResult<ConfigVO> detail(@PathVariable Long id) {
        return ApiResult.success(toVO(configService.detail(id)));
    }

    /**
     * 查看敏感项明文（编辑回显场景）。独立权限点 sys:config:reveal，
     * 与列表/详情脱敏口径分离；非敏感项直接返回原值。审计随 OperLogInterceptor 全局覆盖。
     */
    @GetMapping("/{id}/plaintext")
    @SaCheckPermission("sys:config:reveal")
    public ApiResult<String> plaintext(@PathVariable Long id) {
        SysConfig entity = configService.detail(id);
        String value = entity.getConfigValue();
        if (entity.getIsSensitive() != null && entity.getIsSensitive() == 1
                && StringUtils.hasText(value)) {
            value = configCrypto.decrypt(value);
        }
        return ApiResult.success(value == null ? "" : value);
    }

    @PostMapping
    @SaCheckPermission("sys:config:create")
    public ApiResult<Long> create(@RequestBody ConfigForm form) {
        return ApiResult.success(configService.create(toEntity(form)));
    }

    @PutMapping("/{id}")
    @SaCheckPermission("sys:config:update")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody ConfigForm form) {
        SysConfig entity = toEntity(form);
        entity.setId(id);
        configService.update(entity);
        return ApiResult.success();
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("sys:config:delete")
    public ApiResult<Void> delete(@PathVariable Long id) {
        configService.remove(id);
        return ApiResult.success();
    }

    /**
     * 实体转 VO：敏感项不回显明文，仅给脱敏串；非敏感项原值回显便于编辑。
     */
    private ConfigVO toVO(SysConfig entity) {
        ConfigVO vo = new ConfigVO();
        vo.setId(String.valueOf(entity.getId()));
        vo.setConfigName(entity.getConfigName());
        vo.setConfigKey(entity.getConfigKey());
        vo.setValueType(entity.getValueType());
        vo.setIsSensitive(entity.getIsSensitive());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime() == null ? null : TIME_FMT.format(entity.getCreateTime()));

        if (entity.getIsSensitive() != null && entity.getIsSensitive() == 1) {
            String plain = StringUtils.hasText(entity.getConfigValue())
                    ? configCrypto.decrypt(entity.getConfigValue()) : "";
            vo.setConfigValue(null);
            vo.setValueMasked(ConfigCrypto.mask(plain));
        } else {
            vo.setConfigValue(entity.getConfigValue());
        }
        return vo;
    }

    /**
     * 表单转实体：configValue 为明文，是否加密由 service 按敏感性决定。
     */
    private SysConfig toEntity(ConfigForm form) {
        SysConfig entity = new SysConfig();
        entity.setConfigName(form.getConfigName());
        entity.setConfigKey(form.getConfigKey());
        entity.setConfigValue(form.getConfigValue());
        entity.setValueType(form.getValueType());
        entity.setIsSensitive(form.getIsSensitive());
        entity.setStatus(form.getStatus());
        entity.setRemark(form.getRemark());
        return entity;
    }
}
