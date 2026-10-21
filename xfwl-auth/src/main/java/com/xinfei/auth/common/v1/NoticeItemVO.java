package com.xinfei.auth.common.v1;

import lombok.Data;

import java.io.Serializable;

/**
 * 通知视图（对齐前端 NoticeItem）。
 */
@Data
public class NoticeItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String title;
    private String content;
    private Integer type;
    private String level;
    private Integer publishStatus;
    private Integer isRead;
    private String publishTime;
    private String revokeTime;
}