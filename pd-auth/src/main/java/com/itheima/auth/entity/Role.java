package com.itheima.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色。租户私有，对应 sys_role。
 */
@Data
@TableName("sys_role")
public class Role {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 所属租户ID（由租户拦截器自动写入与过滤） */
    private Long tenantId;

    /** 角色名称 */
    private String name;

    /** 角色编码 */
    private String code;

    /** 功能描述 */
    private String description;

    /** 启用状态：1 启用 0 禁用 */
    private Integer status;

    /** 是否内置角色：1 是 0 否 */
    private Integer readonly;

    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;

    /** 逻辑删除：0 未删 1 已删 */
    @TableLogic
    private Integer deleted;
}
