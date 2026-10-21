package com.xinfei.auth.common.v1;

import lombok.Data;

import java.io.Serializable;

/**
 * 应用表单（对齐前端 AppForm）。
 */
@Data
public class AppForm implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String appName;
    private String appCode;
    private String platform;
    private String appId;
    private String appSecret;
    private String merchantId;
    private String merchantKey;
    private Integer status;
    private String remark;
    private String tenantId;
}