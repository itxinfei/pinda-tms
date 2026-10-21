package com.xinfei.auth.license;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;
import java.util.Optional;

/**
 * License Key 验签器（RSA-SHA256）。
 *
 * <p>license key 格式（类似 JWT，但用 RS256）：
 * <pre>  base64url(payloadJson) + "." + base64url(signature) </pre>
 * 验签公钥来自 classpath {@code license-public.pem}（随包发布，不可篡改签名）；
 * 私钥仅作者本地保管，绝不入库。
 */
public class LicenseVerifier {

    private static final String PUB_RESOURCE = "license-public.pem";
    private static final String ENV_KEY = "XINFEI_LICENSE";
    private static final String[] KEY_FILES = {
            "license.key",
            "config/license.key",
            "/etc/xinfei/license.key"
    };

    private final ObjectMapper mapper = new JsonMapper();
    private final PublicKey publicKey;

    public LicenseVerifier() {
        PublicKey pk = null;
        try {
            pk = PemUtils.publicKey(PemUtils.readClasspath(PUB_RESOURCE));
        } catch (Exception ignored) {
            // 公钥缺失时降级为"未授权"（不影响业务，仅提示）
        }
        this.publicKey = pk;
    }

    /** 从环境变量或文件读取 license key 原文。 */
    public Optional<String> readRawKey() {
        String env = System.getenv(ENV_KEY);
        if (StringUtils.hasText(env)) {
            return Optional.of(env.trim());
        }
        for (String f : KEY_FILES) {
            Path p = Paths.get(f);
            if (Files.exists(p)) {
                try {
                    return Optional.of(Files.readString(p).trim());
                } catch (Exception ignored) {
                    // 继续尝试下一个
                }
            }
        }
        return Optional.empty();
    }

    /** 校验签名与有效期；失败返回 empty。 */
    public Optional<DecodedLicense> verify() {
        if (publicKey == null) {
            return Optional.empty();
        }
        return readRawKey().flatMap(raw -> {
            try {
                String[] parts = raw.split("\\.");
                if (parts.length != 2) {
                    return Optional.empty();
                }
                String payloadB64 = parts[0];
                byte[] sig = Base64.getUrlDecoder().decode(parts[1]);

                Signature s = Signature.getInstance("SHA256withRSA");
                s.initVerify(publicKey);
                s.update(payloadB64.getBytes(StandardCharsets.US_ASCII));
                if (!s.verify(sig)) {
                    return Optional.empty();
                }

                JsonNode node = mapper.readTree(Base64.getUrlDecoder().decode(payloadB64));
                long expiresAt = node.has("expiresAt") ? node.get("expiresAt").asLong() : 0L;
                if (expiresAt != 0 && expiresAt < System.currentTimeMillis()) {
                    return Optional.empty();
                }

                DecodedLicense d = new DecodedLicense();
                d.setLicensee(node.path("licensee").asText(null));
                d.setType(node.path("type").asText(null));
                d.setExpiresAt(expiresAt == 0 ? null : expiresAt);
                d.setInstances(node.path("instances").asInt(1));
                return Optional.of(d);
            } catch (Exception e) {
                return Optional.empty();
            }
        });
    }
}
