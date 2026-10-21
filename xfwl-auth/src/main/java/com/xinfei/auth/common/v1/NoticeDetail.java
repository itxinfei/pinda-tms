package com.xinfei.auth.common.v1;

import lombok.Data;

import java.io.Serializable;

/**
 * 通知详情（对齐前端 NoticeDetail）。
 */
@Data
public class NoticeDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String title;
    private String content;
    private Integer type;
    private String level;
    private Integer publishStatus;
    private String targetUserIds;
    private String publisherName;
    private String publishTime;
}