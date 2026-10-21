package com.xinfei.auth.common.v1;

import lombok.Data;

import java.io.Serializable;

/**
 * 在线会话视图（在线用户页）。token 只回显掩码，防止完整凭证外泄。
 */
@Data
public class OnlineVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 会话 token 掩码（前 8 位 + …） */
    private String tokenMask;

    /** 完整 token（仅本响应内部流转，用于踢人提交；不落前端持久化） */
    private String token;

    private Long userId;

    private String account;

    private String name;

    private String tenantCode;

    /** 会话创建时间 yyyy-MM-dd HH:mm:ss */
    private String loginTime;
}
