package com.xinfei.auth.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知公告。
 */
@Data
@TableName("sys_notice")
public class SysNotice {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String title;
    private String content;
    /** 通知类型 */
    private Integer type;
    /** 等级 info/warning/success/error */
    private String level;
    /** 发布状态 0草稿 1已发布 2已撤回 */
    private Integer status;
    /** 目标类型 1全部 2指定用户 */
    private Integer targetType;
    /** 目标用户ID（逗号分隔） */
    private String targetUsers;
    private Long publisherId;
    private String publisherName;
    private LocalDateTime publishTime;
    private LocalDateTime revokeTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}