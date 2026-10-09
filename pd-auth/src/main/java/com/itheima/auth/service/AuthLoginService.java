package com.itheima.auth.service;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.itheima.auth.common.BizException;
import com.itheima.auth.config.MybatisPlusConfigure;
import com.itheima.auth.dto.LoginDTO;
import com.itheima.auth.dto.LoginParamDTO;
import com.itheima.auth.entity.AuthTenant;
import com.itheima.auth.entity.AuthUser;
import com.itheima.auth.mapper.AuthTenantMapper;
import com.itheima.auth.mapper.AuthUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 登录编排：失败锁定 -> 验证码 -> 租户 -> 账号密码 -> Sa-Token 签发与会话写入。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthLoginService {

    private final CaptchaService captchaService;
    private final AuthTenantMapper tenantMapper;
    private final AuthUserMapper userMapper;
    private final StringRedisTemplate redisTemplate;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${pinda.login.max-fail:5}")
    private int maxLoginFail;

    @Value("${pinda.login.lock-seconds:1800}")
    private long lockSeconds;

    private static final String FAIL_KEY_PREFIX = "pinda:login:fail:";

    /**
     * 账号登录
     */
    public LoginDTO login(LoginParamDTO param, String ip) {
        String failKey = FAIL_KEY_PREFIX + param.getTenantCode() + ":" + param.getAccount() + ":" + ip;

        // 1. 失败锁定检查
        String failCount = redisTemplate.opsForValue().get(failKey);
        if (failCount != null && Integer.parseInt(failCount) >= maxLoginFail) {
            log.warn("账号已临时锁定: tenant={}, account={}, ip={}", param.getTenantCode(), param.getAccount(), ip);
            throw new BizException("登录失败次数过多，账号已临时锁定，请" + lockSeconds / 60 + "分钟后再试");
        }

        // 2. 验证码校验（一次性，通过即作废）
        captchaService.verify(param.getKey(), param.getCode());

        // 3. 定位租户
        AuthTenant tenant = tenantMapper.selectOne(Wrappers.<AuthTenant>lambdaQuery()
                .eq(AuthTenant::getCode, param.getTenantCode()));
        if (tenant == null || !Boolean.TRUE.equals(tenant.getStatus())) {
            throw BizException.validFail("企业编码不存在或已停用，请核对后重试");
        }
        if (tenant.getExpireTime() != null && LocalDateTime.now().isAfter(tenant.getExpireTime())) {
            throw new BizException("企业服务已到期，请联系平台续费");
        }

        // 4. 查询用户（租户内账号）
        AuthUser user = userMapper.selectOne(Wrappers.<AuthUser>lambdaQuery()
                .eq(AuthUser::getTenantId, tenant.getId())
                .eq(AuthUser::getAccount, param.getAccount()));

        // 5. 密码校验。用户不存在与密码错误返回相同信息并计失败，防止探测有效账号
        boolean matched = user != null && passwordEncoder.matches(param.getPassword(), user.getPassword());
        if (!matched) {
            recordLoginFail(failKey);
            log.warn("登录失败: tenant={}, account={}, ip={}", param.getTenantCode(), param.getAccount(), ip);
            throw BizException.validFail("账号或密码错误");
        }

        // 6. 用户状态与密码有效期
        if (!Boolean.TRUE.equals(user.getStatus())) {
            throw BizException.validFail("账号已被禁用，请联系企业管理员");
        }
        if (user.getPasswordExpireTime() != null && LocalDateTime.now().isAfter(user.getPasswordExpireTime())) {
            throw new BizException("密码已过期，请修改密码或联系管理员重置");
        }

        // 7. Sa-Token 签发，并把业务身份写入 token-session，供网关与业务服务读取
        StpUtil.login(user.getId());
        SaSession session = StpUtil.getTokenSession();
        session.set(MybatisPlusConfigure.SESSION_TENANT_ID, user.getTenantId());
        session.set("tenantCode", tenant.getCode());
        session.set("account", user.getAccount());
        session.set("name", user.getName());
        // org_id 可空，SaSession 底层 ConcurrentHashMap 不接受 null 值，判空后再写
        if (user.getOrgId() != null) {
            session.set("orgId", user.getOrgId());
        }

        // 8. 更新最后登录时间、清零失败计数
        AuthUser update = new AuthUser();
        update.setId(user.getId());
        update.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(update);
        redisTemplate.delete(failKey);

        log.info("用户登录成功: tenant={}, account={}, userId={}, ip={}",
                param.getTenantCode(), user.getAccount(), user.getId(), ip);

        return LoginDTO.builder()
                .token(StpUtil.getTokenValue())
                .userId(user.getId())
                .account(user.getAccount())
                .name(user.getName())
                .avatar(user.getAvatar())
                .orgId(user.getOrgId())
                .tenantId(user.getTenantId())
                .permissionsList(List.of())
                .build();
    }

    /**
     * 登出：销毁当前会话
     */
    public void logout() {
        StpUtil.logout();
    }

    /**
     * 记录一次登录失败：计数 +1 并刷新锁定窗口
     */
    private void recordLoginFail(String failKey) {
        Long count = redisTemplate.opsForValue().increment(failKey);
        if (count != null && count == 1L) {
            redisTemplate.expire(failKey, Duration.ofSeconds(lockSeconds));
        }
    }

}
