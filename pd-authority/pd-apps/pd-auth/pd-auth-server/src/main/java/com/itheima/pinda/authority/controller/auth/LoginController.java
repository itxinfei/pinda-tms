package com.itheima.pinda.authority.controller.auth;


import com.itheima.pinda.auth.utils.JwtUserInfo;
import com.itheima.pinda.authority.dto.auth.LoginDTO;
import com.itheima.pinda.authority.dto.auth.LoginParamDTO;
import com.itheima.pinda.authority.biz.service.auth.ValidateCodeService;
import com.itheima.pinda.authority.biz.service.auth.impl.AuthManager;
import cn.hutool.extra.servlet.ServletUtil;
import com.itheima.pinda.base.BaseController;
import com.itheima.pinda.base.R;
import com.itheima.pinda.exception.code.ExceptionCode;
import com.itheima.pinda.exception.BizException;
import com.wf.captcha.base.Captcha;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


/**
 * 客户端获取token
 * jwt token管理
 */
@RestController
@RequestMapping("/anno")
@Api(value = "UserAuthController", tags = "登录")
@Slf4j
public class LoginController extends BaseController {

    @Autowired
    private AuthManager authManager;

    @Autowired
    private ValidateCodeService validateCodeService;

    /**
     * 当前激活的 profile（由 Spring 注入，非 System.getProperty），
     * 用于替代 GET /anno/token 里取空串的旧判断
     */
    @Value("${spring.profiles.active:}")
    private String activeProfile;

    /**
     * GET /anno/token 免验证码登录开关，默认 false（默认拒绝）。
     * 仅非生产环境排障时可临时显式开启，prod 下无论如何都拒绝。
     */
    @Value("${pinda.anno.get-token.enabled:false}")
    private boolean getTokenEnabled;

    /** 请求头中携带 token 的字段名，须与网关 authentication.user.header-name 配置一致 */
    private static final String TOKEN_HEADER = "token";

    /**
     * 登录认证
     */
    @ApiOperation(value = "登录", notes = "登录")
    @PostMapping(value = "/login")
    public R<LoginDTO> login(@Validated @RequestBody LoginParamDTO login, HttpServletRequest request) throws BizException {
        log.info("account={}", login.getAccount());
        if (this.validateCodeService.check(login.getKey(), login.getCode())) {
            // 传入客户端 IP，用于撞库失败锁定
            return this.authManager.login(login.getAccount(), login.getPassword(), ServletUtil.getClientIP(request));
        }
        log.warn("登录失败: 验证码校验不通过, account={}", login.getAccount());
        return this.fail(ExceptionCode.JWT_USER_INVALID);
    }

    /**
     * 租户登录
     *
     * @param login
     * @return
     * @throws BizException
     */
    @ApiOperation(value = "登录", notes = "登录")
    @PostMapping(value = "/loginTx")
    public R<LoginDTO> loginTx(@Validated @RequestBody LoginParamDTO login, HttpServletRequest request) throws BizException {
        log.info("account={}", login.getAccount());
        if (this.validateCodeService.check(login.getKey(), login.getCode())) {
            // 传入客户端 IP，用于撞库失败锁定
            return this.authManager.login(login.getAccount(), login.getPassword(), ServletUtil.getClientIP(request));
        }
        log.warn("登录失败: 验证码校验不通过, account={}", login.getAccount());
        return this.fail(ExceptionCode.JWT_USER_INVALID);
    }

    /**
     * 已废弃的 GET 免验证码登录（历史上仅供测试使用）。
     * 安全策略：
     * 1) prod 环境一律拒绝；
     * 2) 其余环境默认也关闭，只有显式配置 pinda.anno.get-token.enabled=true 才放行（默认拒绝）；
     * 3) 旧实现用 System.getProperty 取 profile，部署形态下取空串导致生产防护失效，现改用 Spring 注入。
     */
    @ApiOperation(value = "已废弃，默认关闭", notes = "已废弃，默认关闭")
    @GetMapping(value = "/token")
    @Deprecated
    public R<LoginDTO> tokenTx(@RequestParam(value = "account") String account,
                               @RequestParam(value = "password") String password,
                               HttpServletRequest request) throws BizException {
        // 生产环境禁止使用 GET 方式登录，防止凭证通过 URL / 访问日志泄露
        if ("prod".equalsIgnoreCase(activeProfile)) {
            return R.fail("生产环境不支持该登录方式，请使用POST /login");
        }
        // 非生产环境默认同样拒绝，需显式开关（默认 false）才临时开启
        if (!getTokenEnabled) {
            return R.fail("该登录方式已默认关闭，请使用POST /login");
        }
        return this.authManager.login(account, password, ServletUtil.getClientIP(request));
    }

    /**
     * 登出：吊销当前 token。
     * 从 token 请求头读取完整 token，登记到 Redis 吊销黑名单（TTL 对齐 token 剩余有效期），
     * 网关后续请求命中黑名单即按未登录处理。
     */
    @ApiOperation(value = "登出", notes = "登出并吊销当前token")
    @PostMapping(value = "/logout")
    public R<Boolean> logout(HttpServletRequest request) {
        String token = request.getHeader(TOKEN_HEADER);
        this.authManager.invalidUserToken(token);
        return this.success(true);
    }


    /**
     * 验证token
     *
     * @param token
     * @return
     * @throws Exception
     */
    @ApiOperation(value = "验证token", notes = "验证token")
    @GetMapping(value = "/verify")
    public R<JwtUserInfo> verify(@RequestParam(value = "token") String token) throws BizException {
        return this.success(this.authManager.validateUserToken(token));
    }

    /**
     * 验证验证码
     *
     * @param key  验证码唯一uuid key
     * @param code 验证码，验证用户提交的验证码
     * @return
     * @throws BizException
     */
    @ApiOperation(value = "验证验证码", notes = "验证验证码")
    @GetMapping(value = "/check")
    public R<Boolean> check(@RequestParam(value = "key") String key, @RequestParam(value = "code") String code) throws BizException {
        return this.success(this.validateCodeService.check(key, code));
    }

    /**
     * 为前端系统生成验证码
     *
     * @param key
     * @param response
     * @throws IOException 修改一下数据类型
     */
    @ApiOperation(value = "验证码", notes = "验证码")
    @GetMapping(value = "/captcha", produces = "image/png")
    public void captcha(@RequestParam(value = "key") String key, HttpServletResponse response) throws IOException {
        log.info("验证码key：" + key);
        this.validateCodeService.create(key, response);
    }

}
