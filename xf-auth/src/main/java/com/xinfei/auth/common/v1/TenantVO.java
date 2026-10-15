package com.xinfei.auth.common.v1;

import lombok.Data;

import java.io.Serializable;

/**
 * 租户分页/详情视图（对齐前端 TenantItem）。
 */
@Data
public class TenantVO implements Serializable {

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
    private String expireTime;
    private String createTime;
    private String updateTime;
}
