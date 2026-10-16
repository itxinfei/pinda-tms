package com.xinfei.auth.controller;

import com.xinfei.auth.common.ClientIps;
import com.xinfei.auth.common.R;
import com.xinfei.auth.common.BizException;
import com.xinfei.auth.dto.LoginDTO;
import com.xinfei.auth.dto.LoginParamDTO;
import com.xinfei.auth.entity.SysOperationLog;
import com.xinfei.auth.service.AuthLoginService;
import com.xinfei.auth.service.CaptchaService;
import com.xinfei.auth.service.v1.LogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@RestController
@RequestMapping("/anno")
@RequiredArgsConstructor
public class AnonController {

    private final CaptchaService captchaService;
    private final AuthLoginService loginService;
    private final LogService logService;

    /**
     * 图形（算术）验证码
     */
    @GetMapping("/captcha")
    public void captcha(@RequestParam String key, HttpServletResponse response) {
        captchaService.create(key, response);
    }

    /**
     * 账号登录（成功/失败均落 LOGIN 审计，SYS-06；失败原因取业务异常消息，不落密码等敏感入参）
     */
    @PostMapping("/login")
    public R<LoginDTO> login(@Valid @RequestBody LoginParamDTO param, HttpServletRequest request) {
        String ip = ClientIps.resolve(request);
        try {
            LoginDTO dto = loginService.login(param, ip);
            recordLogin(ip, param.getAccount(), dto.getUserId(), "登录", 1, null);
            return R.success(dto);
        } catch (BizException e) {
            recordLogin(ip, param.getAccount(), null, "登录", 0, e.getMessage());
            throw e;
        } catch (Exception e) {
            // 非业务异常（如 Redis 抖动）也要留痕，但对外不泄露细节
            log.error("登录异常: account={}", param.getAccount(), e);
            recordLogin(ip, param.getAccount(), null, "登录", 0, "系统异常，请稍后重试");
            throw e;
        }
    }

    /**
     * 登出（有登录态才留痕；匿名重复登出静默通过）
     */
    @PostMapping("/logout")
    public R<Void> logout(HttpServletRequest request) {
        Long userId = null;
        String account = null;
        try {
            if (cn.dev33.satoken.stp.StpUtil.isLogin()) {
                userId = cn.dev33.satoken.stp.StpUtil.getLoginIdAsLong();
                Object acc = cn.dev33.satoken.stp.StpUtil.getTokenSession().get("account");
                account = acc == null ? null : String.valueOf(acc);
            }
        } catch (Exception ignore) {
            // 无会话上下文
        }
        loginService.logout();
        if (userId != null) {
            recordLogin(ClientIps.resolve(request), account, userId, "登出", 1, null);
        }
        return R.success();
    }

    /** 登录/登出审计落库：失败不打断登录主流程。
     *  operatorName 存账号（2110 迁移口径 login_log.account→operator_name，前端按它检索）。 */
    private void recordLogin(String ip, String account, Long userId,
                             String action, int status, String errorMsg) {
        try {
            SysOperationLog row = new SysOperationLog();
            row.setLogType(SysOperationLog.TYPE_LOGIN);
            row.setModule("auth");
            row.setActionType(action);
            row.setTitle("用户" + action);
            row.setContent(action + (status == 1 ? "成功" : "失败"));
            row.setOperatorId(userId);
            row.setOperatorName(account == null ? "" : account);
            row.setRequestUri("/anno/" + (action.equals("登出") ? "logout" : "login"));
            row.setRequestMethod("POST");
            row.setIp(ip);
            row.setStatus(status);
            row.setErrorMsg(errorMsg == null ? "" : errorMsg);
            logService.record(row);
        } catch (Exception e) {
            log.warn("登录审计落库失败（不影响登录）: {}", e.getMessage());
        }
    }
}
