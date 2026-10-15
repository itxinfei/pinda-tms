package com.xinfei.auth.common.v1;

import lombok.Data;

import java.io.Serializable;

/**
 * 租户表单（编辑，对齐前端 TenantForm）。
 */
@Data
public class TenantForm implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String code;
    private String contactName;
    private String contactEmail;
    private String contactPhone;
    private String domain;
    private String logo;
    private Long planId;
    private Integer status;
    private String remark;
    /** ISO 时间字符串，如 2026-10-15T00:00:00 或 2026-10-15 00:00:00 */
    private String expireTime;
}
