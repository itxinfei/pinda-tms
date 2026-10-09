package com.itheima.pinda.authority.biz.service.auth.impl;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import com.itheima.pinda.authority.biz.service.auth.ValidateCodeService;
import com.itheima.pinda.common.constant.CacheKey;
import com.itheima.pinda.exception.BizException;
import com.wf.captcha.ArithmeticCaptcha;
import com.wf.captcha.ChineseCaptcha;
import com.wf.captcha.GifCaptcha;
import com.wf.captcha.SpecCaptcha;
import com.wf.captcha.base.Captcha;

import net.oschina.j2cache.CacheChannel;
import net.oschina.j2cache.CacheObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * 验证码服务
 */
@Slf4j
@Service
public class ValidateCodeServiceImpl implements ValidateCodeService {

    /** 同一验证码允许的最大错误次数，达到即作废该验证码 */
    private static final int MAX_CAPTCHA_FAIL = 5;
    /** 验证码失败计数保留时长（秒），不短于验证码有效期即可 */
    private static final long CAPTCHA_FAIL_TTL = 300L;

    @Autowired
    private CacheChannel cache;

    @Override
    public void create(String key, HttpServletResponse response) throws IOException {
        if (StringUtils.isBlank(key)) {
            throw BizException.validFail("验证码key不能为空");
        }

        response.setContentType(MediaType.IMAGE_PNG_VALUE);
        response.setHeader(HttpHeaders.PRAGMA, "No-cache");
        response.setHeader(HttpHeaders.CACHE_CONTROL, "No-cache");
        response.setDateHeader(HttpHeaders.EXPIRES, 0L);

        Captcha captcha = new ArithmeticCaptcha(115, 42);
        captcha.setCharType(2);

        // 同一 key 重新生成验证码时，清零旧的失败计数
        cache.evict(CacheKey.CAPTCHA_FAIL, key);
        cache.set(CacheKey.CAPTCHA, key, StringUtils.lowerCase(captcha.text()));
        captcha.out(response.getOutputStream());
    }


    @Override
    public boolean check(String key, String value) {
        if (StringUtils.isBlank(value)) {
            throw BizException.validFail("请输入验证码");
        }
        //根据key从缓存中获取验证码
        CacheObject cacheObject = cache.get(CacheKey.CAPTCHA, key);
        if (cacheObject.getValue() == null) {
            // 验证码已过期或已被作废，一并清理失败计数
            cache.evict(CacheKey.CAPTCHA_FAIL, key);
            throw BizException.validFail("验证码已过期");
        }

        //比对验证码
        if (StringUtils.equalsIgnoreCase(value, String.valueOf(cacheObject.getValue()))) {
            //验证通过：立即失效验证码，并清零失败计数（一次性使用）
            cache.evict(CacheKey.CAPTCHA, key);
            cache.evict(CacheKey.CAPTCHA_FAIL, key);
            return true;
        }

        // 验证失败：失败计数 +1，达到上限立即作废该验证码，防止经 /anno/check 穷举
        int failCount = readInt(cache.get(CacheKey.CAPTCHA_FAIL, key)) + 1;
        if (failCount >= MAX_CAPTCHA_FAIL) {
            cache.evict(CacheKey.CAPTCHA, key);
            cache.evict(CacheKey.CAPTCHA_FAIL, key);
            throw BizException.validFail("验证码错误次数过多，请重新获取验证码");
        }
        cache.set(CacheKey.CAPTCHA_FAIL, key, failCount, CAPTCHA_FAIL_TTL);
        throw BizException.validFail("验证码不正确");
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

    private Captcha createCaptcha(String type) {
        Captcha captcha = null;
        if (StringUtils.equalsIgnoreCase(type, "gif")) {
            captcha = new GifCaptcha(115, 42, 4);
        } else if (StringUtils.equalsIgnoreCase(type, "png")) {
            captcha = new SpecCaptcha(115, 42, 4);
        } else if (StringUtils.equalsIgnoreCase(type, "arithmetic")) {
            captcha = new ArithmeticCaptcha(115, 42);
        } else if (StringUtils.equalsIgnoreCase(type, "chinese")) {
            captcha = new ChineseCaptcha(115, 42);
        }
        captcha.setCharType(2);
        log.debug(captcha.getClass().getSimpleName());
        return captcha;
    }

    private void setHeader(HttpServletResponse response, String type) {
        if (StringUtils.equalsIgnoreCase(type, "gif")) {
            response.setContentType(MediaType.IMAGE_GIF_VALUE);
        } else {
            response.setContentType(MediaType.IMAGE_PNG_VALUE);
        }
        response.setHeader(HttpHeaders.PRAGMA, "No-cache");
        response.setHeader(HttpHeaders.CACHE_CONTROL, "No-cache");
        response.setDateHeader(HttpHeaders.EXPIRES, 0L);
    }
}
