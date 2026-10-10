package com.itheima.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 租户（企业）。平台级表，全局唯一，登录时凭 code 定位租户。
 */
@Data
@TableName("sys_tenant")
public class AuthTenant {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 企业编码（登录标识，全局唯一） */
    private String code;

    /** 企业名称 */
    private String name;

    /** 启用状态：1 启用 0 禁用 */
    private Integer status;

    /** 租户到期时间，为空表示长期有效 */
    private LocalDateTime expireTime;

    /** 联系人 */
    private String contactName;

    /** 联系电话 */
    private String contactPhone;

    /** logo URL */
    private String logo;

    /** 企业简介 */
    private String description;

    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;

    /** 逻辑删除：0 未删 1 已删 */
    @TableLogic
    private Integer deleted;
}
