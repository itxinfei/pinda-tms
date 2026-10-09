package com.itheima.pinda.auth.server.utils;

import com.itheima.pinda.auth.server.properties.AuthServerProperties;
import com.itheima.pinda.auth.utils.JwtHelper;
import com.itheima.pinda.auth.utils.JwtUserInfo;
import com.itheima.pinda.auth.utils.Token;
import com.itheima.pinda.exception.BizException;

import com.itheima.pinda.auth.server.properties.AuthServerProperties;
import com.itheima.pinda.auth.utils.JwtHelper;
import com.itheima.pinda.auth.utils.JwtUserInfo;
import com.itheima.pinda.auth.utils.Token;
import com.itheima.pinda.exception.BizException;
import lombok.AllArgsConstructor;

/**
 * jwt token 工具
 *
 */
@AllArgsConstructor
public class JwtTokenServerUtils {
    /**
     * 认证服务端使用，如 authority-server
     * 生成和 解析token
     */
    private AuthServerProperties authServerProperties;

    /**
     * 生成token
     *
     * @param jwtInfo
     * @param expire
     * @return
     * @throws BizException
     */
    public Token generateUserToken(JwtUserInfo jwtInfo, Integer expire) throws BizException {
        AuthServerProperties.TokenInfo userTokenInfo = authServerProperties.getUser();
        if (expire == null || expire <= 0) {
            expire = userTokenInfo.getExpire();
        }
        return JwtHelper.generateUserToken(jwtInfo, userTokenInfo.getPriKey(), expire);
    }

    /**
     * 解析token
     *
     * @param token
     * @return
     * @throws BizException
     */
    public JwtUserInfo getUserInfo(String token) throws BizException {
        AuthServerProperties.TokenInfo userTokenInfo = authServerProperties.getUser();
        return JwtHelper.getJwtFromToken(token, userTokenInfo.getPubKey());
    }

    /**
     * 获取 token 剩余有效秒数（登出设置黑名单 TTL 用）
     *
     * @param token token
     * @return 剩余秒数；过期或非法返回 0
     */
    public long getRemainingSeconds(String token) {
        AuthServerProperties.TokenInfo userTokenInfo = authServerProperties.getUser();
        return JwtHelper.getRemainingSeconds(token, userTokenInfo.getPubKey());
    }


}
