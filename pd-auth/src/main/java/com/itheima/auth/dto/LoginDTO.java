package com.itheima.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 登录成功返回：token、用户概要、权限码列表。
 */
@Data
@Builder
public class LoginDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String token;

    private Long userId;

    private String account;

    private String name;

    private String avatar;

    private Long orgId;

    private Long tenantId;

    /** 功能权限码列表，本批返回空集合 */
    private List<String> permissionsList;
}
