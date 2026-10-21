package com.xinfei.auth.common.v1;

import lombok.Data;

import java.io.Serializable;

/**
 * 字典项分页列表项（对齐前端 DictItem）。
 * tagType 存语义全称（primary/success/warning/danger/info），前端 API 兼容读取历史单字母。
 */
@Data
public class DictItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    /** 所属字典编码（由 dict_type_id 反查填充） */
    private String dictCode;
    private String label;
    private String value;
    private Integer status;
    /** 排序（实体列为 sort_value） */
    private Integer sort;
    private String tagType;
    private String remark;
}
