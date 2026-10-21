package com.xinfei.auth.license;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * PEM 密钥解析工具（RSA）。仅做去头、去换行、Base64 解码，
 * 不依赖 BouncyCastle，JDK 原生即可。
 */
public final class PemUtils {

    private PemUtils() {
    }

    private static byte[] parse(String pem) {
        String b64 = pem
                .replaceAll("-----BEGIN [A-Z ]+-----", "")
                .replaceAll("-----END [A-Z ]+-----", "")
                .replaceAll("\\s+", "");
        return Base64.getDecoder().decode(b64);
    }

    public static PublicKey publicKey(String pem) throws Exception {
        X509EncodedKeySpec spec = new X509EncodedKeySpec(parse(pem));
        return KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    public static PrivateKey privateKey(String pem) throws Exception {
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(parse(pem));
        return KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    /** 从 classpath 读取公钥资源文本（license-public.pem 已随包发布）。 */
    public static String readClasspath(String resource) throws Exception {
        try (var in = PemUtils.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("缺少公钥资源: " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
