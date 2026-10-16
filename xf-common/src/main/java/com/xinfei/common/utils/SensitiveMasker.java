package com.xinfei.common.utils;

import java.util.regex.Pattern;

/**
 * 日志敏感信息脱敏工具（T-SEC-01/E2：日志中禁止出现明文密码/验证码/token/手机号/身份证）
 *
 * <p>供网关请求日志与各业务服务日志统一复用：对待记录的文本先经 {@link #mask(String)}
 * 再输出。覆盖口径（《69》E2）：password / captcha / code / token 键值对、
 * 11 位手机号（1[3-9] 开头）、18 位身份证号。</p>
 *
 * <p>非 Spring 组件，纯静态工具，无状态。</p>
 */
public final class SensitiveMasker {

    /** password/token/secret 类键值对（query 串 / JSON / yml 风格），值整体替换为 *** */
    private static final Pattern KV_SENSITIVE = Pattern.compile(
            "(?i)((?:password|passwd|pwd|token|captcha|secret|access_?key|api_?key)\\s*[=:]\\s*)(\"?)([^\"&\\s,}]+)\\2");

    /** 验证码/code 键值对（泛化键名较易误伤，仅匹配常见写法） */
    private static final Pattern KV_CODE = Pattern.compile(
            "(?i)((?:captcha|verify_?code|sms_?code|code)\\s*[=:]\\s*)(\"?)([A-Za-z0-9]{4,8})\\2");

    /** 11 位手机号（1[3-9] 开头），保留前 3 后 4 */
    private static final Pattern MOBILE = Pattern.compile("(?<![0-9])(1[3-9]\\d)\\d{4}(\\d{4})(?![0-9])");

    /** 18 位身份证号（含末位 X），保留前 6 后 4 */
    private static final Pattern ID_CARD = Pattern.compile(
            "(?<![0-9Xx])(\\d{6})\\d{8}(\\d{3}[0-9Xx])(?![0-9Xx])");

    private SensitiveMasker() {
    }

    /**
     * 对文本做统一脱敏：键值对敏感键 → ***；手机号 → 138****1234；身份证 → 110101********1234。
     *
     * @param text 原始文本（如 query 串、请求头值），null 原样返回
     * @return 脱敏后的文本
     */
    public static String mask(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String out = KV_SENSITIVE.matcher(text).replaceAll("$1$2***$2");
        out = KV_CODE.matcher(out).replaceAll("$1$2***$2");
        out = MOBILE.matcher(out).replaceAll("$1****$2");
        out = ID_CARD.matcher(out).replaceAll("$1********$2");
        return out;
    }
}
