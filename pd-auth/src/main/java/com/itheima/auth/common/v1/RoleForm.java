package com.itheima.auth.common.v1;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 角色新建/编辑入参（对齐前端 RoleForm）。
 * deptIds/dataScope 属 P2 数据权限，本期忽略；description 映射 remark。
 */
@Data
public class RoleForm implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String code;
    private String name;
    private Integer sort;
    private Integer status;
    private Integer dataScope;
    private List<String> deptIds;
    private String remark;
}
