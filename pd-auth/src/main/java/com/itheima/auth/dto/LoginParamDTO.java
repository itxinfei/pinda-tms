package com.itheima.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录入参：企业编码 + 账号 + 密码 + 验证码。
 */
@Data
public class LoginParamDTO {

    @NotBlank(message = "企业编码不能为空")
    private String tenantCode;

    @NotBlank(message = "账号不能为空")
    private String account;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "验证码KEY不能为空")
    private String key;

    @NotBlank(message = "验证码不能为空")
    private String code;
}
