package com.xinfei.auth.common.v1;

import lombok.Data;

import java.io.Serializable;

/**
 * 参数配置新增/修改表单（youlai 契约）。
 *
 * <p>敏感值 {@code configValue} 传明文（后端加密落库）；编辑敏感项留空表示不修改原值。
 * {@code isSensitive} 新增后不可变更。</p>
 */
@Data
public class ConfigForm implements Serializable {

    private static final long serialVersionUID = 1L;

    private String configName;

    /** 参数键名（字母/数字/点/下划线/中划线，租户内唯一） */
    private String configKey;

    /** 参数值明文；编辑敏感项留空表示不修改 */
    private String configValue;

    /** 值类型：string/number/boolean/json */
    private String valueType;

    /** 是否敏感：1 是 0 否 */
    private Integer isSensitive;

    /** 状态：1 启用 0 停用 */
    private Integer status;

    private String remark;
}
