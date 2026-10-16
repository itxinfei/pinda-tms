package com.xinfei.auth.common.v1;

import lombok.Data;

import java.io.Serializable;

/**
 * 参数配置列表/详情 VO（youlai 契约）。
 *
 * <p>敏感项 {@code configValue} 恒为 null，仅返回 {@code valueMasked} 脱敏串；
 * 非敏感项回显原值供编辑。</p>
 */
@Data
public class ConfigVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 配置 ID（雪花 ID 字符串，前端按 string 消费避免精度丢失） */
    private String id;

    private String configName;

    private String configKey;

    /** 非敏感项回显原值；敏感项为 null */
    private String configValue;

    /** 敏感值脱敏展示串（仅敏感项返回） */
    private String valueMasked;

    /** 值类型：string/number/boolean/json */
    private String valueType;

    /** 是否敏感：1 是 0 否（新增后不可改） */
    private Integer isSensitive;

    /** 状态：1 启用 0 停用 */
    private Integer status;

    private String remark;

    private String createTime;
}
