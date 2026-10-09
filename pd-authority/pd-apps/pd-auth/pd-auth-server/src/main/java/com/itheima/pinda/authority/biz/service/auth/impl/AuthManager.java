package com.itheima.pinda.authority.biz.service.auth.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.itheima.pinda.auth.server.utils.JwtTokenServerUtils;
import com.itheima.pinda.auth.utils.JwtUserInfo;
import com.itheima.pinda.auth.utils.Token;
import com.itheima.pinda.authority.biz.service.auth.ResourceService;
import com.itheima.pinda.authority.biz.service.auth.UserService;
import com.itheima.pinda.authority.dto.auth.LoginDTO;
import com.itheima.pinda.authority.dto.auth.ResourceQueryDTO;
import com.itheima.pinda.authority.dto.auth.UserDTO;
import com.itheima.pinda.authority.entity.auth.Resource;
import com.itheima.pinda.authority.entity.auth.User;
import com.itheima.pinda.base.R;
import com.itheima.pinda.common.constant.CacheKey;
import com.itheima.pinda.dozer.DozerUtils;
import com.itheima.pinda.exception.BizException;
import com.itheima.pinda.exception.code.ExceptionCode;
import lombok.extern.slf4j.Slf4j;
import net.oschina.j2cache.CacheChannel;
import net.oschina.j2cache.CacheObject;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 账号管理服务
 */
@Service
@Slf4j
public class AuthManager {

    /** 连续登录失败达到该次数即锁定 */
    private static final int MAX_LOGIN_FAIL = 5;
    /** 失败计数/锁定窗口（秒）：每次失败刷新，30 分钟无失败自动解锁 */
    private static final long LOGIN_FAIL_LOCK_SECONDS = 1800L;
    /** 用户级吊销（禁用）登记保留时长（秒），覆盖在途 token 最长有效期即可 */
    private static final long USER_REVOKE_SECONDS = 86400L;

    @Autowired
    private JwtTokenServerUtils jwtTokenServerUtils;
    @Autowired
    private UserService userService;
    @Autowired
    private ResourceService resourceService;
    @Autowired
    private DozerUtils dozer;
    @Autowired
    private CacheChannel cacheChannel;

    /**
     * 账号登录
     *
     * @param account  账号
     * @param password 密码（明文，由调用方通过 HTTPS 传入）
     * @param ip       客户端 IP，用于撞库锁定
     */
    public R<LoginDTO> login(String account, String password, String ip) {
        // 登录验证
        R<User> result = checkUser(account, password, ip);
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
        log.info("用户登录成功: account={}, userId={}, ip={}", user.getAccount(), user.getId(), ip);
        return R.success(loginDTO);
    }

    /**
     * 生成jwt token
     *
     * @param user
     * @return
     */
    private Token generateUserToken(User user) {
        JwtUserInfo userInfo = new JwtUserInfo(user.getId(), user.getAccount(), user.getName(), user.getOrgId(), user.getStationId());

        Token token = this.jwtTokenServerUtils.generateUserToken(userInfo, null);
        log.info("token生成成功，前缀: {}***", token.getToken() != null && token.getToken().length() > 8 ? token.getToken().substring(0, 8) : "***");
        return token;
    }

    /**
     * 登录验证（真实链路）：
     * <pre>
     * 1. 按「账号 + IP」检查失败锁定
     * 2. 查询用户、校验密码（BCrypt，兼容存量 MD5 并平滑升级）
     * 3. 用户禁用、密码过期校验
     * 4. 成功清零失败计数
     * </pre>
     */
    private R<User> checkUser(String account, String password, String ip) {
        String failKey = CacheKey.buildKey(account, ip);

        // 1. 锁定检查：计数达到阈值即直接拒绝
        int failCount = readInt(cacheChannel.get(CacheKey.LOGIN_FAIL, failKey));
        if (failCount >= MAX_LOGIN_FAIL) {
            log.warn("账号登录已锁定: account={}, ip={}, 失败次数={}", account, ip, failCount);
            return R.fail(ExceptionCode.JWT_USER_INVALID.getCode(),
                    "登录失败次数过多，账号已临时锁定，请" + (LOGIN_FAIL_LOCK_SECONDS / 60) + "分钟后再试");
        }

        // 2. 查询用户
        User user = this.userService.getOne(Wrappers.<User>lambdaQuery().eq(User::getAccount, account));

        // 3. 密码校验。用户不存在同样计失败，避免接口被用来探测有效账号
        boolean passwordMatched = user != null && matchesPassword(password, user.getPassword());
        if (!passwordMatched) {
            recordLoginFail(failKey, failCount);
            log.warn("登录失败: account={}, ip={}", account, ip);
            return R.fail(ExceptionCode.JWT_USER_INVALID);
        }

        // 密码正确但仍是历史 MD5：本次登录通过，同时平滑升级为 BCrypt
        if (!isBCryptPassword(user.getPassword())) {
            upgradeToBCrypt(user, password);
        }

        // 4. 用户禁用
        if (user.getStatus() != null && !user.getStatus()) {
            return R.fail(ExceptionCode.JWT_USER_INVALID.getCode(), "用户已被禁用，请联系管理员");
        }

        // 5. 密码过期
        if (user.getPasswordExpireTime() != null && LocalDateTime.now().isAfter(user.getPasswordExpireTime())) {
            return R.fail(ExceptionCode.JWT_USER_INVALID.getCode(), "用户密码已过期，请修改密码或联系管理员重置");
        }

        // 6. 登录成功：清零失败计数
        cacheChannel.evict(CacheKey.LOGIN_FAIL, failKey);
        return R.success(user);
    }

    /**
     * 密码校验：库存为 BCrypt 用 BCrypt 校验，否则按存量 MD5 校验
     */
    private boolean matchesPassword(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null) {
            return false;
        }
        if (isBCryptPassword(storedPassword)) {
            try {
                return BCrypt.checkpw(rawPassword, storedPassword);
            } catch (Exception e) {
                return false;
            }
        }
        return DigestUtils.md5Hex(rawPassword).equals(storedPassword);
    }

    /**
     * 判断库存密码是否为 BCrypt 哈希（标准 BCrypt 固定 60 字符，$2 开头）
     */
    private boolean isBCryptPassword(String storedPassword) {
        return storedPassword != null && storedPassword.length() == 60
                && (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$"));
    }

    /**
     * 把存量 MD5 密码平滑升级为 BCrypt。升级失败不阻断本次登录，下次登录仍走 MD5 兼容。
     */
    private void upgradeToBCrypt(User user, String rawPassword) {
        try {
            String bcryptPassword = BCrypt.hashpw(rawPassword, BCrypt.gensalt());
            User update = new User();
            update.setId(user.getId());
            update.setPassword(bcryptPassword);
            this.userService.updateById(update);
            user.setPassword(bcryptPassword);
            log.info("用户密码已由 MD5 平滑升级为 BCrypt: userId={}", user.getId());
        } catch (Exception e) {
            log.error("密码升级 BCrypt 失败（不影响本次登录）: userId={}, err={}", user.getId(), e.getMessage());
        }
    }

    /**
     * 记录一次登录失败：计数 +1 并刷新锁定窗口，达到阈值后封顶不再增长
     */
    private void recordLoginFail(String failKey, int currentCount) {
        int next = currentCount + 1;
        if (next > MAX_LOGIN_FAIL) {
            next = MAX_LOGIN_FAIL;
        }
        cacheChannel.set(CacheKey.LOGIN_FAIL, failKey, next, LOGIN_FAIL_LOCK_SECONDS);
    }

    /**
     * 安全读取缓存中的整数计数
     */
    private int readInt(CacheObject cacheObject) {
        Object value = cacheObject == null ? null : cacheObject.getValue();
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public JwtUserInfo validateUserToken(String token) throws BizException {
        return this.jwtTokenServerUtils.getUserInfo(token);
    }

    /**
     * 登出：把 token 的 SHA-256 摘要写入吊销黑名单，TTL 精确对齐到 token 的 exp，
     * 网关在解析 token 后检查该名单，命中即按未登录处理。
     */
    public void invalidUserToken(String token) throws BizException {
        if (token == null || token.trim().isEmpty()) {
            return;
        }
        String digest = DigestUtils.sha256Hex(token);
        long remainingSeconds = this.jwtTokenServerUtils.getRemainingSeconds(token);
        if (remainingSeconds <= 0) {
            // token 已过期或非法，本身已不可用，无需登记
            return;
        }
        cacheChannel.set(CacheKey.TOKEN_BLACKLIST, digest, "1", remainingSeconds);
        log.info("token 已吊销并加入黑名单, TTL={}秒", remainingSeconds);
    }

    /**
     * 吊销指定用户的所有存量 token（禁用用户时调用）。
     * 无需逐个定位 token，网关按 userId 命中即拒绝。
     */
    public void revokeUser(Long userId) {
        cacheChannel.set(CacheKey.TOKEN_BLACKLIST_USER, String.valueOf(userId), "1", USER_REVOKE_SECONDS);
        log.info("用户已登记吊销，其存量 token 将立即失效: userId={}", userId);
    }

    /**
     * 解除用户级吊销登记（重新启用用户时调用）
     */
    public void allowUser(Long userId) {
        cacheChannel.evict(CacheKey.TOKEN_BLACKLIST_USER, String.valueOf(userId));
        log.info("用户吊销登记已解除: userId={}", userId);
    }

}
