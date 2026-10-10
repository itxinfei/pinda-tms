package com.itheima.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 账号-角色绑定。租户私有，对应 sys_user_role（仅业务四列，无审计列、无逻辑删除）。
 */
@Data
@TableName("sys_user_role")
public class AuthUserRole {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 所属租户ID（由租户拦截器自动写入与过滤） */
    private Long tenantId;

    /** 角色ID */
    private Long roleId;

    /** 用户ID */
    private Long userId;
}
