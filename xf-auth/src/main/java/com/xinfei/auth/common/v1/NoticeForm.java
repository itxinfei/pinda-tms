package com.xinfei.auth.common.v1;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 通知表单（对齐前端 NoticeForm）。
 */
@Data
public class NoticeForm implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String title;
    private String content;
    private Integer type;
    private String level;
    private Integer status;
    private List<Long> targetUsers;
    private Integer targetType;
}