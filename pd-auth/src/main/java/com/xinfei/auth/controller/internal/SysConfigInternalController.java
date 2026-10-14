package com.xinfei.auth.controller.internal;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xinfei.auth.common.BizException;
import com.xinfei.auth.entity.SysConfig;
import com.xinfei.auth.service.SysConfigService;
import com.xinfei.DTO.SysConfigDTO;
import com.xinfei.common.config.dynamic.ConfigCrypto;
import com.xinfei.common.utils.PageResponse;
import com.xinfei.common.utils.Result;
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

import java.util.List;

/**
 * 参数配置内部端点（CFG-01）：仅供 BFF（pd-web-manager）经 Same-Token 调用，路径与 ConfigFeign 对齐。
 * 读端点裸返回 DTO / PageResponse；写端点返回 Result，业务校验失败转 Result.error（HTTP 200）。
 */
@RestController
@RequestMapping("/internal/config")
@RequiredArgsConstructor
public class SysConfigInternalController {

    private final SysConfigService configService;
    private final ConfigCrypto configCrypto;

    @GetMapping("/page")
    public PageResponse<SysConfigDTO> page(@RequestParam("page") Integer page,
                                           @RequestParam("pageSize") Integer pageSize,
                                           @RequestParam(value = "keywords", required = false) String keywords,
                                           @RequestParam(value = "status", required = false) Integer status) {
        IPage<SysConfig> result = configService.page(page, pageSize, keywords, status);
        List<SysConfigDTO> items = result.getRecords().stream().map(this::toDTO).toList();
        return PageResponse.<SysConfigDTO>builder()
                .counts(result.getTotal())
                .pagesize((int) result.getSize())
                .pages(result.getPages())
                .page((int) result.getCurrent())
                .items(items)
                .build();
    }

    @GetMapping("/{id}")
    public SysConfigDTO detail(@PathVariable("id") Long id) {
        return toDTO(configService.detail(id));
    }

    @PostMapping
    public Result create(@RequestBody SysConfigDTO dto) {
        try {
            configService.create(toEntity(dto));
            return Result.ok();
        } catch (BizException e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result update(@PathVariable("id") Long id, @RequestBody SysConfigDTO dto) {
        try {
            SysConfig entity = toEntity(dto);
            entity.setId(id);
            configService.update(entity);
            return Result.ok();
        } catch (BizException e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result remove(@PathVariable("id") Long id) {
        try {
            configService.remove(id);
            return Result.ok();
        } catch (BizException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 实体转 DTO：敏感项不回显明文，仅给脱敏串；非敏感项原值回显便于编辑。
     */
    private SysConfigDTO toDTO(SysConfig entity) {
        SysConfigDTO dto = new SysConfigDTO();
        dto.setId(String.valueOf(entity.getId()));
        dto.setConfigName(entity.getConfigName());
        dto.setConfigKey(entity.getConfigKey());
        dto.setValueType(entity.getValueType());
        dto.setIsSensitive(entity.getIsSensitive());
        dto.setStatus(entity.getStatus());
        dto.setRemark(entity.getRemark());

        if (entity.getIsSensitive() != null && entity.getIsSensitive() == 1) {
            String plain = StringUtils.hasText(entity.getConfigValue())
                    ? configCrypto.decrypt(entity.getConfigValue()) : "";
            dto.setConfigValue(null);
            dto.setValueMasked(ConfigCrypto.mask(plain));
        } else {
            dto.setConfigValue(entity.getConfigValue());
            dto.setValueMasked(null);
        }
        return dto;
    }

    /**
     * DTO 转实体：configValue 为明文（是否加密由 service 按敏感性决定）。
     */
    private SysConfig toEntity(SysConfigDTO dto) {
        SysConfig entity = new SysConfig();
        if (StringUtils.hasText(dto.getId())) {
            entity.setId(Long.valueOf(dto.getId()));
        }
        entity.setConfigName(dto.getConfigName());
        entity.setConfigKey(dto.getConfigKey());
        entity.setConfigValue(dto.getConfigValue());
        entity.setValueType(dto.getValueType());
        entity.setIsSensitive(dto.getIsSensitive());
        entity.setStatus(dto.getStatus());
        entity.setRemark(dto.getRemark());
        return entity;
    }
}
