package com.itheima.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 参数配置（平台全局，tenant_id 固定 0）。敏感值 configValue 存 AES 密文（{@code enc:} 前缀）。
 * 物理删除（不做逻辑删），避免与 uk_tenant_key 唯一键冲突。
 */
@Data
@TableName("sys_config")
public class SysConfig {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 租户ID，一期固定 0=平台全局 */
    private Long tenantId;

    private String configName;

    private String configKey;

    /** 存储值；isSensitive=1 时为 AES 密文 */
    private String configValue;

    /** 值类型：string/number/boolean/json */
    private String valueType;

    /** 是否敏感：1 是（加密入库） 0 否 */
    private Integer isSensitive;

    /** 状态：1 启用 0 停用 */
    private Integer status;

    private String remark;

    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
}
