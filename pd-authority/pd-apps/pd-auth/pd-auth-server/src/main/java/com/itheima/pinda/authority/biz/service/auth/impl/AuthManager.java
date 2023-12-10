package com.itheima.pinda.authority.biz.service.auth.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.itheima.pinda.auth.server.utils.JwtTokenServerUtils;
import com.itheima.pinda.auth.utils.JwtUserInfo;
import com.itheima.pinda.auth.utils.Token;
import com.itheima.pinda.authority.biz.service.auth.ResourceService;
import com.itheima.pinda.authority.biz.service.auth.UserService;
import com.itheima.pinda.authority.biz.utils.TimeUtils;
import com.itheima.pinda.authority.dto.auth.LoginDTO;
import com.itheima.pinda.authority.dto.auth.ResourceQueryDTO;
import com.itheima.pinda.authority.dto.auth.UserDTO;
import com.itheima.pinda.authority.entity.auth.Resource;
import com.itheima.pinda.authority.entity.auth.User;
import com.itheima.pinda.base.R;
import com.itheima.pinda.database.properties.DatabaseProperties;
import com.itheima.pinda.dozer.DozerUtils;
import com.itheima.pinda.exception.BizException;
import com.itheima.pinda.exception.code.ExceptionCode;
import com.itheima.pinda.utils.BizAssert;
import com.itheima.pinda.utils.NumberHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 */
@Service
@Slf4j
public class AuthManager {
    @Autowired
    private JwtTokenServerUtils jwtTokenServerUtils;
    @Autowired
    private UserService userService;
    @Autowired
    private ResourceService resourceService;
    @Autowired
    private DozerUtils dozer;
    @Autowired
    private DatabaseProperties databaseProperties;


    /**
     * 登陆
     *
     * @param account
     * @param password
     * @return
     */
    private R<LoginDTO> simpleLogin(String account, String password) {
        // 2. 验证登录
        R<User> result = this.getUser(account, password);
        if (result.getIsError()) {
            return R.fail(result.getCode(), result.getMsg());
        }
        User user = result.getData();

        // 3, token
        Token token = this.getToken(user);

        List<Resource> resourceList = this.resourceService.findVisibleResource(ResourceQueryDTO.builder().userId(user.getId()).build());
        log.info("resourceList {} {}", resourceList.size(), resourceList);

        List<String> permissionsList = resourceList.stream().filter(item -> item != null).map(Resource::getCode).collect(Collectors.toList());
        log.info("account={}", account);
        log.info("permissionsList={}", permissionsList);
        return R.success(LoginDTO.builder().user(this.dozer.map(user, UserDTO.class)).permissionsList(permissionsList).token(token).build());
    }


    /**
     * 账号登录
     *
     * @param account
     * @param password
     */
    public R<LoginDTO> login(String account, String password) {
        // 登录验证
        R<User> result = checkUser(account, password);
        if (result.getIsError()) {
            return R.fail(result.getCode(), result.getMsg());
        }
        User user = result.getData();

        // 生成jwt token
        Token token = this.generateUserToken(user);

        List<Resource> resourceList = this.resourceService.findVisibleResource(ResourceQueryDTO.builder().userId(user.getId()).build());
        List<String> permissionsList = null;
        if (resourceList != null && resourceList.size() > 0) {
            permissionsList = resourceList.stream().map(Resource::getCode).collect(Collectors.toList());
        }
        //封装数据
        LoginDTO loginDTO = LoginDTO.builder().user(this.dozer.map(user, UserDTO.class)).token(token).permissionsList(permissionsList).build();
        log.info("loginDTO={}", loginDTO);
        return R.success(loginDTO);
    }

    //生成jwt token
    private Token generateUserToken(User user) {
        JwtUserInfo userInfo = new JwtUserInfo(user.getId(), user.getAccount(), user.getName(), user.getOrgId(), user.getStationId());

        Token token = this.jwtTokenServerUtils.generateUserToken(userInfo, null);
        log.info("token={}", token.getToken());
        return token;
    }

    // 登录验证
    private R<User> checkUser(String account, String password) {
        User user = this.userService.getOne(Wrappers.<User>lambdaQuery().eq(User::getAccount, account));

        // 密码加密
        String passwordMd5 = DigestUtils.md5Hex(password);

        if (user == null || !user.getPassword().equals(passwordMd5)) {
            return R.fail(ExceptionCode.JWT_USER_INVALID);
        }

        return R.success(user);
    }

    private Token getToken(User user) {
        JwtUserInfo userInfo = new JwtUserInfo(user.getId(), user.getAccount(), user.getName(), user.getOrgId(), user.getStationId());

        Token token = this.jwtTokenServerUtils.generateUserToken(userInfo, null);
        log.info("token={}", token.getToken());
        return token;
    }

    private R<User> getUser(String account, String password) {
        User user = this.userService.getOne(Wrappers.<User>lambdaQuery().eq(User::getAccount, account));
        // 密码错误
        String passwordMd5 = DigestUtils.md5Hex(password);
        if (user == null) {
            return R.fail(ExceptionCode.JWT_USER_INVALID);
        }


        if (!user.getPassword().equalsIgnoreCase(passwordMd5)) {
            this.userService.updatePasswordErrorNumById(user.getId());
            return R.fail("用户名或密码错误!");
        }

        // 密码过期
        if (user.getPasswordExpireTime() != null) {
            BizAssert.gt(LocalDateTime.now(), user.getPasswordExpireTime(), "用户密码已过期，请修改密码或者联系管理员重置!");
        }

        // 用户禁用
        BizAssert.isTrue(user.getStatus(), "用户被禁用，请联系管理员！");

        // 用户锁定
        Integer maxPasswordErrorNum = NumberHelper.getOrDef(user.getPasswordErrorNum(), 0);
        Integer passwordErrorNum = NumberHelper.getOrDef(user.getPasswordErrorNum(), 0);
        if (maxPasswordErrorNum > 0 && passwordErrorNum > maxPasswordErrorNum) {
            log.info("当前错误次数{}, 最大次数:{}", passwordErrorNum, maxPasswordErrorNum);

            LocalDateTime passwordErrorLockTime = TimeUtils.getPasswordErrorLockTime("0");
            log.info("passwordErrorLockTime={}", passwordErrorLockTime);
            if (passwordErrorLockTime.isAfter(user.getPasswordErrorLastTime())) {
                return R.fail("密码连续输错次数已达到%s次,用户已被锁定~", maxPasswordErrorNum);
            }
        }

        // 错误次数清空
        this.userService.resetPassErrorNum(user.getId());
        return R.success(user);
    }

    public JwtUserInfo validateUserToken(String token) throws BizException {
        return this.jwtTokenServerUtils.getUserInfo(token);
    }

    public void invalidUserToken(String token) throws BizException {

    }

}
