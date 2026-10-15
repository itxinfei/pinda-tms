package com.xinfei.auth.common.v1;

import lombok.Data;

import java.io.Serializable;

/**
 * 字典类型新建/编辑入参（对齐前端 DictTypeForm）。
 */
@Data
public class DictTypeForm implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    /** 字典编码（实体列为 code） */
    private String dictCode;
    private Integer status;
    private String remark;
}
