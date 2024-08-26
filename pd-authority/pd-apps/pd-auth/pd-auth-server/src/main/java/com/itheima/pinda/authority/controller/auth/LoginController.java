package com.itheima.pinda.authority.controller.auth;


import com.itheima.pinda.auth.utils.JwtUserInfo;
import com.itheima.pinda.authority.dto.auth.LoginDTO;
import com.itheima.pinda.authority.dto.auth.LoginParamDTO;
import com.itheima.pinda.authority.biz.service.auth.ValidateCodeService;
import com.itheima.pinda.authority.biz.service.auth.impl.AuthManager;
import com.itheima.pinda.base.BaseController;
import com.itheima.pinda.base.R;
import com.itheima.pinda.exception.BizException;
import com.wf.captcha.base.Captcha;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
     * 登录认证
     */
    @ApiOperation(value = "登录", notes = "登录")
    @PostMapping(value = "/login")
    public R<LoginDTO> login(@Validated @RequestBody LoginParamDTO login) throws BizException {
        log.info("account={}", login.getAccount());
        if (this.validateCodeService.check(login.getKey(), login.getCode())) {
            return this.authManager.login(login.getAccount(), login.getPassword());

        }
        log.info("登录成功！");
        return this.success(null);
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
    public R<LoginDTO> loginTx(@Validated @RequestBody LoginParamDTO login) throws BizException {
        log.info("account={}", login.getAccount());
        if (this.validateCodeService.check(login.getKey(), login.getCode())) {
            return this.authManager.login(login.getAccount(), login.getPassword());
        }
        return this.success(null);
    }

    /**
     * 刷新token
     *
     * @return
     * @throws Exception
     */
    @ApiOperation(value = "仅供测试使用", notes = "仅供测试使用")
    @GetMapping(value = "/token")
    @Deprecated
    public R<LoginDTO> tokenTx(@RequestParam(value = "account") String account, @RequestParam(value = "password") String password) throws BizException {
        return this.authManager.login(account, password);
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
