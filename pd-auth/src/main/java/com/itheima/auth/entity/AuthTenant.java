package com.itheima.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 租户（企业）。平台级表，全局唯一，登录时凭 code 定位租户。
 */
@Data
@TableName("pd_auth_tenant")
public class AuthTenant {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 企业编码（登录标识，全局唯一） */
    private String code;

    /** 企业名称 */
    private String name;

    /** 启用状态：true 启用 false 禁用 */
    private Boolean status;

    /** 租户到期时间，为空表示长期有效 */
    private LocalDateTime expireTime;

    private Long createUser;
    private LocalDateTime createTime;
    private Long updateUser;
    private LocalDateTime updateTime;
}
