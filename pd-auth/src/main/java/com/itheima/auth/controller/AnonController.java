package com.itheima.auth.controller;

import com.itheima.auth.common.R;
import com.itheima.auth.dto.LoginDTO;
import com.itheima.auth.dto.LoginParamDTO;
import com.itheima.auth.service.AuthLoginService;
import com.itheima.auth.service.CaptchaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 匿名访问入口：验证码、登录、登出。
 */
@RestController
@RequestMapping("/anno")
@RequiredArgsConstructor
public class AnonController {

    private final CaptchaService captchaService;
    private final AuthLoginService loginService;

    /**
     * 图形（算术）验证码
     */
    @GetMapping("/captcha")
    public void captcha(@RequestParam String key, HttpServletResponse response) {
        captchaService.create(key, response);
    }

    /**
     * 账号登录
     */
    @PostMapping("/login")
    public R<LoginDTO> login(@Valid @RequestBody LoginParamDTO param, HttpServletRequest request) {
        return R.success(loginService.login(param, resolveClientIp(request)));
    }

    /**
     * 登出
     */
    @PostMapping("/logout")
    public R<Void> logout() {
        loginService.logout();
        return R.success();
    }

    /**
     * 提取客户端真实 IP：优先网关/代理注入的转发头，逐级取第一个非 unknown 地址。
     */
    private String resolveClientIp(HttpServletRequest request) {
        String[] headers = {"X-Real-IP", "X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP"};
        for (String header : headers) {
            String value = request.getHeader(header);
            if (StringUtils.hasText(value) && !"unknown".equalsIgnoreCase(value)) {
                int comma = value.indexOf(',');
                return comma > 0 ? value.substring(0, comma).trim() : value.trim();
            }
        }
        return request.getRemoteAddr();
    }
}
