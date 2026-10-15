package com.xinfei.auth.common.v1;

import lombok.Data;

import java.io.Serializable;

/**
 * 字典项新建/编辑入参（对齐前端 DictItemForm，remark 为后端补充字段）。
 */
@Data
public class DictItemForm implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    /** 所属字典编码（以路径参数 dictCode 为准） */
    private String dictCode;
    private String label;
    private String value;
    private Integer status;
    private Integer sort;
    private String tagType;
    private String remark;
}
