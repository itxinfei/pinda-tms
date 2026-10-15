package com.xinfei.auth.common.v1;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 部门视图（对齐前端 DeptItem）。
 */
@Data
public class DeptVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String parentId;
    private String name;
    private String code;
    private Integer sort;
    private Integer status;
    private String treePath;
    private String createTime;
    private String updateTime;
    private List<DeptVO> children = new ArrayList<>();
}