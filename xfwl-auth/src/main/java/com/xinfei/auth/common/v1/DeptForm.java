package com.xinfei.auth.common.v1;

import lombok.Data;

import java.io.Serializable;

/**
 * 部门表单（对齐前端 DeptForm）。
 */
@Data
public class DeptForm implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String code;
    private String parentId;
    private Integer sort;
    private Integer status;
}