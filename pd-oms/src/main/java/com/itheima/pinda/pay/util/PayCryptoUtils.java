package com.itheima.pinda.pay.util;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * 支付签名/加解密工具
 *
 * <p>提供微信支付 APIv3 与支付宝 RSA2 所需的：
 * RSA-SHA256 私钥签名、公钥验签、AES-256-GCM 解密。
 * 仅依赖 JDK 内置加密能力，无需引入外部 SDK。</p>
 */
@Slf4j
public class PayCryptoUtils {

    private PayCryptoUtils() {
    }

    /**
     * RSA-SHA256 私钥签名（微信 APIv3 / 支付宝 RSA2 通用）
     *
     * @param privateKeyPem PKCS8 格式 PEM 私钥（去掉 BEGIN/END 头尾）
     * @param content       待签名内容
     * @return Base64 签名
     */
    public static String rsaSha256Sign(String privateKeyPem, String content) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(privateKeyPem.replaceAll("\\s", ""));
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(spec);
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(content.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception e) {
            log.error("[支付加密] RSA-SHA256 签名失败", e);
            throw new IllegalStateException("RSA-SHA256 签名失败", e);
        }
    }

    /**
     * RSA-SHA256 公钥验签（支付宝 RSA2 / 微信平台证书验签通用）
     *
     * @param publicKeyPem X509 格式 PEM 公钥（去掉 BEGIN/END 头尾）
     * @param content      待验签内容
     * @param signature    Base64 签名
     * @return 是否验签通过
     */
    public static boolean rsaSha256Verify(String publicKeyPem, String content, String signature) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(publicKeyPem.replaceAll("\\s", ""));
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(spec);
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(publicKey);
            verifier.update(content.getBytes(StandardCharsets.UTF_8));
            return verifier.verify(Base64.getDecoder().decode(signature));
        } catch (Exception e) {
            log.error("[支付加密] RSA-SHA256 验签失败", e);
            return false;
        }
    }

    /**
     * AES-256-GCM 解密（微信支付 APIv3 回调 resource 解密）
     *
     * @param apiKey    APIv3 密钥（32 字节）
     * @param nonce     nonce
     * @param associatedData 附加数据
     * @param ciphertext Base64 密文（尾部含 16 字节认证标签）
     * @return 明文
     */
    public static String aesGcmDecrypt(String apiKey, String nonce, String associatedData, String ciphertext) {
        try {
            byte[] keyBytes = apiKey.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            byte[] cipherBytes = Base64.getDecoder().decode(ciphertext);
            // GCM 认证标签固定 16 字节，附加在密文尾部
            GCMParameterSpec gcmSpec = new GCMParameterSpec(128, nonce.getBytes(StandardCharsets.UTF_8));
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
            if (associatedData != null && !associatedData.isEmpty()) {
                cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
            }
            byte[] plain = cipher.doFinal(cipherBytes);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("[支付加密] AES-GCM 解密失败", e);
            throw new IllegalStateException("AES-GCM 解密失败", e);
        }
    }
}
