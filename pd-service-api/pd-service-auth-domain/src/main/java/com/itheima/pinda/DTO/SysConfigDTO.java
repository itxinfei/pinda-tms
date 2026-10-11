package com.itheima.pinda.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 参数配置 DTO（CFG-01）：管理端 CRUD 与 Feign 契约。
 *
 * <p>ID 用 String 承载雪花 ID，避免前端 JS Number 精度丢失；
 * 敏感项 {@code configValue} 读取时不回显（null），仅回传 {@code valueMasked} 脱敏串，
 * 编辑提交留空表示不修改原值。</p>
 */
@Data
@Schema(description = "参数配置")
public class SysConfigDTO {

    @Schema(description = "参数ID（雪花ID字符串）")
    private String id;

    @Schema(description = "参数名称")
    private String configName;

    @Schema(description = "参数键名，如 baidu.map.ak")
    private String configKey;

    @Schema(description = "参数值明文（写入用）；敏感项读取时为 null")
    private String configValue;

    @Schema(description = "敏感值脱敏展示串（仅读取，敏感项返回）")
    private String valueMasked;

    @Schema(description = "值类型：string/number/boolean/json")
    private String valueType;

    @Schema(description = "是否敏感：1是 0否")
    private Integer isSensitive;

    @Schema(description = "状态：1启用 0停用")
    private Integer status;

    @Schema(description = "备注")
    private String remark;
}
