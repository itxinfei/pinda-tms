package com.itheima.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 岗位（职务）。隶属于某个组织，租户私有。
 */
@Data
@TableName("sys_station")
public class CoreStation {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 所属租户ID（由租户拦截器自动写入与过滤） */
    private Long tenantId;

    /** 岗位名称 */
    private String name;

    /** 所属组织ID */
    private Long orgId;

    /** 启用状态：1 启用 0 禁用 */
    private Integer status;

    /** 描述 */
    private String description;

    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;

    /** 逻辑删除：0 未删 1 已删 */
    @TableLogic
    private Integer deleted;
}
