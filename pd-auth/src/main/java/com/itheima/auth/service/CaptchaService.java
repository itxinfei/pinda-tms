package com.itheima.auth.service;

import com.itheima.auth.common.BizException;
import com.wf.captcha.SpecCaptcha;
import com.wf.captcha.base.Captcha;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.Duration;

/**
 * 验证码服务：字符图形验证码 PNG + Redis 一次性校验，输错上限即作废。
 * 不用 ArithmeticCaptcha：其依赖 Nashorn 脚本引擎，JDK 15+ 已移除。
 */
@Service
@RequiredArgsConstructor
public class CaptchaService {

    private final StringRedisTemplate redisTemplate;

    @Value("${pinda.captcha.ttl:120}")
    private long ttlSeconds;

    @Value("${pinda.captcha.width:115}")
    private int width;

    @Value("${pinda.captcha.height:42}")
    private int height;

    @Value("${pinda.captcha.max-fail:5}")
    private int maxFail;

    private static final String CAPTCHA_KEY_PREFIX = "pinda:captcha:";
    private static final String FAIL_KEY_PREFIX = "pinda:captcha:fail:";

    /**
     * 生成算术验证码并写入响应流（PNG）。
     */
    public void create(String key, HttpServletResponse response) {
        if (!StringUtils.hasText(key)) {
            throw BizException.validFail("验证码key不能为空");
        }

        response.setContentType(MediaType.IMAGE_PNG_VALUE);
        response.setHeader(HttpHeaders.PRAGMA, "no-cache");
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
        response.setDateHeader(HttpHeaders.EXPIRES, 0L);

        // 4 位数字图形验证码（len 固定 4，便于移动端数字键盘输入）
        Captcha captcha = new SpecCaptcha(width, height, 4);
        captcha.setCharType(Captcha.TYPE_ONLY_NUMBER);

        // 同一 key 重新获取：清零失败计数，答案覆盖旧值
        redisTemplate.delete(FAIL_KEY_PREFIX + key);
        redisTemplate.opsForValue().set(CAPTCHA_KEY_PREFIX + key,
                captcha.text().toLowerCase(), Duration.ofSeconds(ttlSeconds));

        try {
            captcha.out(response.getOutputStream());
        } catch (IOException e) {
            throw new BizException(500, "验证码生成失败");
        }
    }

    /**
     * 校验验证码。通过即作废（一次性）；失败累计，达到上限作废。
     */
    public void verify(String key, String code) {
        if (!StringUtils.hasText(code)) {
            throw BizException.validFail("请输入验证码");
        }

        String answer = redisTemplate.opsForValue().get(CAPTCHA_KEY_PREFIX + key);
        if (!StringUtils.hasText(answer)) {
            redisTemplate.delete(FAIL_KEY_PREFIX + key);
            throw BizException.validFail("验证码已过期，请重新获取");
        }

        if (answer.equalsIgnoreCase(code)) {
            redisTemplate.delete(CAPTCHA_KEY_PREFIX + key);
            redisTemplate.delete(FAIL_KEY_PREFIX + key);
            return;
        }

        Long failCount = redisTemplate.opsForValue().increment(FAIL_KEY_PREFIX + key);
        if (failCount != null && failCount == 1L) {
            redisTemplate.expire(FAIL_KEY_PREFIX + key, Duration.ofSeconds(ttlSeconds));
        }
        if (failCount != null && failCount >= maxFail) {
            redisTemplate.delete(CAPTCHA_KEY_PREFIX + key);
            redisTemplate.delete(FAIL_KEY_PREFIX + key);
            throw BizException.validFail("验证码错误次数过多，请重新获取验证码");
        }
        throw BizException.validFail("验证码不正确");
    }
}
