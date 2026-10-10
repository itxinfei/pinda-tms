package com.itheima.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 岗位（职务）。隶属于某个组织，租户私有。
 */
@Data
@TableName("pd_core_station")
public class CoreStation {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 所属租户ID（由租户拦截器自动写入与过滤） */
    private Long tenantId;

    /** 岗位名称 */
    private String name;

    /** 所属组织ID（#pd_core_org） */
    private Long orgId;

    /** 启用状态：true 启用 false 禁用 */
    private Boolean status;

    /** 描述（数据库列 describe_，避开 SQL 保留字 DESCRIBE） */
    @TableField("describe_")
    private String describe;

    private LocalDateTime createTime;
    private Long createUser;
    private LocalDateTime updateTime;
    private Long updateUser;
}
