package com.itheima.pinda.service.sms;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * 短信平台签名工具
 *
 * <p>提供阿里云 RPC、腾讯云 TC3、华为云 AK/SK 等短信平台所需的 HMAC-SHA256 签名能力，
 * 仅依赖 JDK 内置加密，无需引入外部 SDK。</p>
 */
public class SmsSignUtils {

    private SmsSignUtils() {
    }

    /**
     * HMAC-SHA256 摘要（Base64）
     *
     * @param secret 密钥
     * @param data   数据
     * @return Base64 签名
     */
    public static String hmacSha256Base64(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 计算失败", e);
        }
    }

    /**
     * HMAC-SHA256 摘要（Hex）
     *
     * @param secret 密钥
     * @param data   数据
     * @return Hex 签名
     */
    public static String hmacSha256Hex(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return toHex(bytes);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 计算失败", e);
        }
    }

    /**
     * SHA256 摘要（Hex）
     *
     * @param data 数据
     * @return Hex 摘要
     */
    public static String sha256Hex(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return toHex(digest.digest(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA256 计算失败", e);
        }
    }

    /**
     * URL 编码（阿里云 RPC 签名规范）
     *
     * @param value 原始值
     * @return 编码值
     */
    public static String percentEncode(String value) {
        try {
            // 阿里云要求：空格编码为 %20（而非 +）
            return URLEncoder.encode(value, "UTF-8")
                .replace("+", "%20")
                .replace("*", "%2A")
                .replace("%7E", "~");
        } catch (Exception e) {
            throw new IllegalStateException("URL 编码失败", e);
        }
    }

    /**
     * 字节数组转 Hex
     */
    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
