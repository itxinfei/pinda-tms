package com.xinfei.auth.license;

import com.xinfei.auth.common.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;
import java.util.Optional;

/**
 * License 验签器（RSA-SHA256）。
 *
 * <p>授权码格式：{@code base64url(payloadJson) + "." + base64url(signature)}，
 * 签名内容是 payload 的 base64url 字符串（ASCII）。验签公钥来自 classpath
 * {@code license-public.pub}（随包发布，只用于验签，私钥仅在独立签发库保管）。</p>
 *
 * <p>授权码来源优先级：环境变量 {@code XINFEI_LICENSE}（只读，作者/运维注入）
 * &gt; 落盘文件 {@code <xinfei.license.dir>/license.key}（页面激活写入，容器卷持久化）。</p>
 */
@Slf4j
@Component
public class LicenseVerifier {

    public static final String ENV_KEY = "XINFEI_LICENSE";
    public static final String KEY_FILE_NAME = "license.key";
    private static final String PUB_RESOURCE = "license-public.pub";

    private final ObjectMapper mapper = new JsonMapper();
    private final Path keyFile;
    private final PublicKey publicKey;

    public LicenseVerifier(@Value("${xinfei.license.dir:/data/xinfei-license}") String dir) {
        this.keyFile = Paths.get(dir, KEY_FILE_NAME);
        PublicKey pk = null;
        try {
            pk = PemUtils.publicKey(PemUtils.readClasspath(PUB_RESOURCE));
            log.info("[license] 验签公钥已加载：{}", PUB_RESOURCE);
        } catch (Exception e) {
            // 不再静默：公钥缺失会导致授权验签整体不可用，必须在启动日志暴露
            log.error("[license] 验签公钥缺失或无法解析（{}），授权验签不可用，请确认公钥已随包发布",
                    PUB_RESOURCE, e);
        }
        this.publicKey = pk;
    }

    /** 当前授权码来源。 */
    public KeySource currentSource() {
        if (StringUtils.hasText(System.getenv(ENV_KEY))) {
            return KeySource.ENV;
        }
        return Files.isRegularFile(keyFile) ? KeySource.FILE : KeySource.NONE;
    }

    /** 当前是否由环境变量托管（只读，页面不可改）。 */
    public boolean isEnvManaged() {
        return currentSource() == KeySource.ENV;
    }

    /** 读取当前生效的授权码原文：环境变量优先，其次落盘文件。 */
    public Optional<String> readRawKey() {
        String env = System.getenv(ENV_KEY);
        if (StringUtils.hasText(env)) {
            return Optional.of(env.trim());
        }
        try {
            if (Files.isRegularFile(keyFile)) {
                return Optional.of(Files.readString(keyFile, StandardCharsets.UTF_8).trim());
            }
        } catch (IOException e) {
            log.warn("[license] 读取授权文件失败：{}", keyFile, e);
        }
        return Optional.empty();
    }

    /**
     * 校验给定授权码签名并解析载荷。只验签与结构解析，机器绑定/到期由 LicenseService 判定。
     *
     * @throws BizException 公钥未就绪、格式错误、签名不符或载荷非法
     */
    public DecodedLicense verify(String raw) {
        if (publicKey == null) {
            throw new BizException("授权验签公钥未就绪，请联系管理员");
        }
        String value = raw == null ? "" : raw.trim();
        int dot = value.indexOf('.');
        if (dot <= 0 || dot != value.lastIndexOf('.') || dot >= value.length() - 1) {
            throw new BizException("授权码格式不正确，应为 载荷.签名 两段");
        }
        String payloadB64 = value.substring(0, dot);
        byte[] sig;
        byte[] payloadBytes;
        try {
            sig = Base64.getUrlDecoder().decode(value.substring(dot + 1));
            payloadBytes = Base64.getUrlDecoder().decode(payloadB64);
        } catch (IllegalArgumentException e) {
            throw new BizException("授权码格式不正确（Base64 解码失败）");
        }
        try {
            Signature s = Signature.getInstance("SHA256withRSA");
            s.initVerify(publicKey);
            s.update(payloadB64.getBytes(StandardCharsets.US_ASCII));
            if (!s.verify(sig)) {
                throw new BizException("授权码签名校验失败（可能已损坏或非本系统签发）");
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("授权码验签异常：" + e.getMessage());
        }

        try {
            JsonNode node = mapper.readTree(payloadBytes);
            DecodedLicense d = new DecodedLicense();
            d.setLicensee(textOrNull(node, "licensee"));
            d.setType(textOrNull(node, "type"));
            d.setInstances(node.path("instances").asInt(1));
            d.setMachineId(textOrNull(node, "machineId"));
            d.setIssuedAt(node.has("issuedAt") && node.get("issuedAt").isNumber()
                    ? node.get("issuedAt").asLong() : null);
            d.setExpiresAt(node.has("expiresAt") && node.get("expiresAt").isNumber()
                    && node.get("expiresAt").asLong() != 0L ? node.get("expiresAt").asLong() : null);
            d.setAud(textOrNull(node, "aud"));
            if (!StringUtils.hasText(d.getType())) {
                throw new BizException("授权码缺少 type 字段");
            }
            return d;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("授权码载荷解析失败");
        }
    }

    /** 原子写入授权文件（先写临时文件再 ATOMIC_MOVE，避免半写文件被读到）。 */
    public synchronized void saveKey(String raw) {
        Path tmp = null;
        try {
            Files.createDirectories(keyFile.getParent());
            tmp = Files.createTempFile(keyFile.getParent(), "license-", ".tmp");
            Files.writeString(tmp, raw.trim() + System.lineSeparator(), StandardCharsets.UTF_8);
            try {
                Files.move(tmp, keyFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, keyFile, StandardCopyOption.REPLACE_EXISTING);
            }
            tmp = null;
        } catch (IOException e) {
            throw new BizException("授权文件写入失败：" + e.getMessage());
        } finally {
            if (tmp != null) {
                try {
                    Files.deleteIfExists(tmp);
                } catch (IOException ignored) {
                    // 临时文件残留不影响主流程
                }
            }
        }
    }

    /** 删除落盘授权码（调用方须先排除环境变量来源）。 */
    public synchronized boolean deleteKey() {
        try {
            return Files.deleteIfExists(keyFile);
        } catch (IOException e) {
            throw new BizException("授权文件删除失败：" + e.getMessage());
        }
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) {
            return null;
        }
        String s = v.asText("");
        return StringUtils.hasText(s) ? s.trim() : null;
    }

    /** 授权码来源。 */
    public enum KeySource {
        /** 环境变量 XINFEI_LICENSE（只读） */
        ENV,
        /** 页面激活后落盘的文件 */
        FILE,
        /** 无授权 */
        NONE
    }
}
