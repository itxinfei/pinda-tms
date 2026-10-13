package com.itheima.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 角色-菜单/按钮授权（sys_role_menu，租户私有，无逻辑删除）。
 */
@Data
@TableName("sys_role_menu")
public class RoleMenu {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long roleId;

    private Long menuId;
}
