package com.xinfei.auth.common.v1;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 新增租户表单（编辑字段 + 初始化管理员账号，对齐前端 TenantCreateForm）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TenantCreateForm extends TenantForm implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 初始化管理员账号（为空时默认 admin_{code}） */
    private String adminUsername;
}
