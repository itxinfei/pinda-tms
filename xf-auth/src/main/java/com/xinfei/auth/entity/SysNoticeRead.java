package com.xinfei.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通知-已读关系。
 */
@Data
@TableName("sys_notice_read")
public class SysNoticeRead implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long noticeId;
    private Long userId;

    /** 已读时间（《62》§4.3） */
    private LocalDateTime readTime;
}