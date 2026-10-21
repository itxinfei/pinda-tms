package com.xinfei.auth.common.v1;

import lombok.Data;

import java.io.Serializable;

/**
 * 字典类型分页列表项（对齐前端 DictTypeItem：id/name/dictCode/status）。
 */
@Data
public class DictTypeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    /** 字典编码（实体列为 code） */
    private String dictCode;
    private Integer status;
}
