package com.xinfei.auth.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 应用（三方平台配置）。
 */
@Data
@TableName("sys_app")
public class SysApp {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String appName;
    private String appCode;
    /** 平台 WECHAT_MP/WECHAT_MINI/ALIPAY/APPLE/QQ */
    private String platform;
    private String appId;
    private String appSecret;
    private String merchantId;
    private String merchantKey;
    private Integer status;
    private String remark;
    /** 租户ID（平台级配置=0） */
    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}