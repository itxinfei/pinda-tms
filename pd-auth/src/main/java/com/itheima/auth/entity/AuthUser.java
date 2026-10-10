package com.itheima.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录账号。隶属于某个租户，账号在租户内唯一。
 */
@Data
@TableName("sys_user")
public class AuthUser {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 所属租户ID */
    private Long tenantId;

    /** 账号（租户内唯一） */
    private String account;

    /** 姓名 */
    private String name;

    /** 所属组织ID */
    private Long orgId;

    /** 岗位ID */
    private Long stationId;

    private String email;

    private String mobile;

    /** 性别：0 未知 1 男 2 女 */
    private Integer sex;

    /** 启用状态：1 启用 0 禁用 */
    private Integer status;

    private String avatar;

    /** 职务描述，登录后展示 */
    private String workDescribe;

    /** 密码（BCrypt 哈希，固定 60 位） */
    private String password;

    /** 密码过期时间，为空表示不过期 */
    private LocalDateTime passwordExpireTime;

    /** 最后登录时间 */
    private LocalDateTime lastLoginTime;

    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;

    /** 逻辑删除：0 未删 1 已删 */
    @TableLogic
    private Integer deleted;
}
