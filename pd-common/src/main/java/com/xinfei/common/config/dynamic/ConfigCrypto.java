package com.xinfei.common.config.dynamic;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * 参数配置敏感值加解密（AES-256-GCM）与脱敏（CFG-01）。
 *
 * <p>仅依赖 JDK 自带 {@code javax.crypto}，不引入第三方加密库。密文格式：
 * {@code enc:} + Base64( IV(12B) ‖ 密文(含16B GCM Tag) )；{@code enc:} 前缀用于识别密文，
 * 历史明文 / 非敏感值不加前缀，{@link #decrypt} 原样返回，保证向后兼容。</p>
 */
public class ConfigCrypto {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final String CIPHER_PREFIX = "enc:";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SecretKeySpec keySpec;

    public ConfigCrypto(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            throw new IllegalArgumentException("pinda.config.crypto-key 不能为空");
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawKey.trim().getBytes(StandardCharsets.UTF_8));
            this.keySpec = new SecretKeySpec(digest, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("初始化配置加解密器失败", e);
        }
    }

    /**
     * 加密明文。null / 空串原样返回（空值不加密）。
     */
    public String encrypt(String plain) {
        if (plain == null || plain.isEmpty()) {
            return plain;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] cipherText = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);
            return CIPHER_PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("敏感配置加密失败", e);
        }
    }

    /**
     * 解密密文。非 {@code enc:} 前缀的值视为历史明文原样返回；解密失败抛异常（主密钥变更/数据损坏）。
     */
    public String decrypt(String stored) {
        if (stored == null || stored.isEmpty() || !stored.startsWith(CIPHER_PREFIX)) {
            return stored;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(stored.substring(CIPHER_PREFIX.length()));
            byte[] iv = Arrays.copyOfRange(combined, 0, IV_LENGTH);
            byte[] cipherText = Arrays.copyOfRange(combined, IV_LENGTH, combined.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("敏感配置解密失败（主密钥是否变更或数据损坏）", e);
        }
    }

    /**
     * 是否为密文存储值。
     */
    public boolean isEncrypted(String stored) {
        return stored != null && stored.startsWith(CIPHER_PREFIX);
    }

    /**
     * 敏感值脱敏：统一圆点前缀 + 保留后 4 位；长度 ≤4 全部遮掩。
     */
    public static String mask(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        if (value.length() <= 4) {
            return "••••••••";
        }
        return "••••••••" + value.substring(value.length() - 4);
    }
}
