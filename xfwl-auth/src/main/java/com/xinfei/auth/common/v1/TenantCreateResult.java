package com.xinfei.auth.common.v1;

import lombok.Data;

import java.io.Serializable;

/**
 * 新增租户结果（含初始化管理员信息，对齐前端 TenantCreateResult）。
 */
@Data
public class TenantCreateResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private String tenantId;
    private String tenantCode;
    private String tenantName;
    private String adminUsername;
    private String adminInitialPassword;
    private String adminRoleCode;
}
