package com.xinfei.auth.common.v1;

import lombok.Data;

import java.io.Serializable;

/**
 * 租户套餐表单（对齐前端 TenantPlanForm）。
 */
@Data
public class TenantPlanForm implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String code;
    private Integer status;
    private Integer sort;
    private String remark;
}