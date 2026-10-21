package com.xinfei.auth.common.v1;

import lombok.Data;

import java.io.Serializable;

/**
 * 租户下拉/当前租户信息（对齐前端 TenantInfo）。
 */
@Data
public class TenantInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String domain;
}
