package com.itheima.pinda.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会员
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("member_info")
public class Member implements Serializable {

    /**
     * 主键（雪花）
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 手机号（登录名）
     */
    private String phone;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 身份证号
     */
    private String idCardNo;

    /**
     * 身份证验证状态：0未验证 1通过 2未通过
     */
    private Integer idCardVerify;

    /**
     * 状态：1正常 0禁用
     */
    private Integer status;

    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;

    /**
     * 逻辑删除：0未删 1已删
     */
    @TableLogic
    private Integer deleted;

    // 注意：不声明 tenantId 字段。租户列由 TenantLineInnerInterceptor 在 insert 时自动补入
    // （从网关 tenantId 请求头 / TenantContextHolder 解析）；若实体声明 tenantId 且为 null，
    // 插件会因列已存在而跳过填充，导致 NOT NULL 写入失败。
}
