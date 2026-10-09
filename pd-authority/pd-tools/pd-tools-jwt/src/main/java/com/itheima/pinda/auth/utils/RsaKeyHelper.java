package com.itheima.pinda.auth.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Rsa key 帮助类
 *
 * <p><b>安全说明（2026-10-07）：</b>
 * 历史私钥 {@code client/pri.key} 曾随源码提交，私钥<b>已经泄露</b>，任何能拿到源码的人
 * 都可伪造任意用户的合法 JWT。生产环境<b>必须轮换密钥对</b>，并通过下列环境变量之一
 * 把新私钥注入到进程外部，<b>不要再依赖 classpath 中的旧私钥</b>：
 * <ol>
 *   <li>{@code PINDA_JWT_PRI_KEY}      ：直接提供 PKCS#8 私钥内容（PEM 文本或裸 Base64）</li>
 *   <li>{@code PINDA_JWT_PRI_KEY_PATH} ：容器外私钥文件路径（DER 二进制或 PEM 文本均可）</li>
 * </ol>
 * 公钥侧同理：{@code PINDA_JWT_PUB_KEY} / {@code PINDA_JWT_PUB_KEY_PATH}。
 * 只有在以上环境变量都未配置时，才回退读取 classpath（保留仅为本地开发与平滑过渡）。
 */
public class RsaKeyHelper {

    /** 私钥内容（PEM 或 Base64 文本）环境变量名 */
    private static final String ENV_PRI_KEY_CONTENT = "PINDA_JWT_PRI_KEY";
    /** 私钥外部文件路径环境变量名 */
    private static final String ENV_PRI_KEY_PATH = "PINDA_JWT_PRI_KEY_PATH";
    /** 公钥内容（PEM 或 Base64 文本）环境变量名 */
    private static final String ENV_PUB_KEY_CONTENT = "PINDA_JWT_PUB_KEY";
    /** 公钥外部文件路径环境变量名 */
    private static final String ENV_PUB_KEY_PATH = "PINDA_JWT_PUB_KEY_PATH";

    /**
     * 获取公钥,用于解析token
     *
     * @param filename classpath 回退路径（如 client/pub.key）
     */
    public PublicKey getPublicKey(String filename) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] keyBytes = resolveKeyBytes(filename, ENV_PUB_KEY_CONTENT, ENV_PUB_KEY_PATH);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }

    /**
     * 获取密钥 用于生成token
     *
     * @param filename classpath 回退路径（如 client/pri.key）
     */
    public PrivateKey getPrivateKey(String filename) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] keyBytes = resolveKeyBytes(filename, ENV_PRI_KEY_CONTENT, ENV_PRI_KEY_PATH);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(spec);
    }

    /**
     * 按优先级定位密钥的 DER 字节：
     * <pre>
     * 1. 环境变量 envContent      直接提供 PEM / Base64 文本内容
     * 2. 环境变量 envPath         指向容器外密钥文件（DER 或 PEM）
     * 3. fallback 为 file: 前缀   读取该外部文件
     * 4. fallback 为已存在文件路径 读取该外部文件
     * 5. 兜底                       从 classpath 读取 fallback（历史 client/*.key）
     * </pre>
     *
     * @param fallback   配置中传入的路径（历史 classpath 路径）
     * @param envContent 密钥内容环境变量名
     * @param envPath    密钥文件路径环境变量名
     */
    private byte[] resolveKeyBytes(String fallback, String envContent, String envPath) throws IOException {
        // 1. 环境变量直接提供密钥内容
        String content = System.getenv(envContent);
        if (content != null && !content.trim().isEmpty()) {
            return decodePemOrBase64(content);
        }

        // 2. 环境变量提供外部文件路径
        String extPath = System.getenv(envPath);
        if (extPath != null && !extPath.trim().isEmpty()) {
            return normalizeKeyBytes(Files.readAllBytes(Paths.get(stripFilePrefix(extPath.trim()))));
        }

        if (fallback == null || fallback.trim().isEmpty()) {
            throw new IOException("未配置任何 RSA 密钥来源（环境变量与 classpath 路径均为空）");
        }
        String path = fallback.trim();

        // 3. file: 前缀显式指定外部文件
        if (path.startsWith("file:")) {
            return normalizeKeyBytes(Files.readAllBytes(Paths.get(stripFilePrefix(path))));
        }

        // 4. 传入路径在文件系统中真实存在，按外部文件读取
        File file = new File(path);
        if (file.isFile()) {
            return normalizeKeyBytes(Files.readAllBytes(file.toPath()));
        }

        // 5. 回退 classpath（本地开发/平滑过渡，生产不应走到这里）
        try (InputStream in = this.getClass().getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new IOException("密钥不存在，classpath 与文件系统均未找到: " + path);
            }
            return normalizeKeyBytes(readAll(in));
        }
    }

    /**
     * 把密钥字节归一化为 DER：
     * DER 二进制以 ASN.1 SEQUENCE 标记 0x30 开头，直接返回；
     * 否则视为 PEM / Base64 文本，剥离 PEM 头尾与空白后解码。
     */
    private static byte[] normalizeKeyBytes(byte[] raw) {
        if (raw != null && raw.length > 0 && (raw[0] & 0xFF) == 0x30) {
            return raw;
        }
        return decodePemOrBase64(new String(raw, StandardCharsets.UTF_8));
    }

    /**
     * 解析 PEM（含 -----BEGIN/END----- 头尾）或裸 Base64 文本为 DER 字节
     */
    private static byte[] decodePemOrBase64(String pemOrBase64) {
        String cleaned = pemOrBase64
                .replaceAll("-----BEGIN[^-]*-----", "")
                .replaceAll("-----END[^-]*-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(cleaned);
    }

    private static String stripFilePrefix(String path) {
        return path.startsWith("file:") ? path.substring("file:".length()) : path;
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int len;
        while ((len = in.read(buffer)) != -1) {
            bos.write(buffer, 0, len);
        }
        return bos.toByteArray();
    }

}
