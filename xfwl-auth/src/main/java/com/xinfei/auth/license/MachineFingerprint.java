package com.xinfei.auth.license;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Enumeration;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 机器指纹采集器。授权绑定对象是<b>运行 xfwl-auth 的服务器宿主</b>（非管理员浏览器），
 * 故指纹只在后端容器内采集。容器 ID / 容器内 MAC 重建即变，不能直接作为指纹。
 *
 * <p>因子按稳定性固定优先级取用（只用非空项，按 {@code key=value\n} 拼接后 SHA-256）：</p>
 * <ol>
 *   <li>显式配置 {@code xinfei.license.node-id}（特殊环境人工指定，最高优先）；</li>
 *   <li>宿主机 machine-id：compose 把宿主 {@code /etc/machine-id} 只读挂到
 *       {@code /data/host/machine-id}（首选，重建容器不变），回落容器内 machine-id；</li>
 *   <li>DMI product_uuid：{@code /sys/class/dmi/id/product_uuid}（sysfs 默认透传宿主）；</li>
 *   <li>以上皆空时回落首个稳定网卡 MAC（并打 WARN；容器重建 MAC 可能变，仅兜底）。</li>
 * </ol>
 *
 * <p>指纹不是秘密（页面会展示给客户），授权防伪仍由 RSA 签名保证；指纹只决定"这枚码能在哪台机器用"。</p>
 */
@Slf4j
@Component
public class MachineFingerprint {

    private static final String[] MACHINE_ID_FILES = {
            "/data/host/machine-id",
            "/etc/machine-id",
            "/var/lib/dbus/machine-id"
    };
    private static final String PRODUCT_UUID = "/sys/class/dmi/id/product_uuid";

    private final String explicitNodeId;
    private volatile String fingerprint;

    public MachineFingerprint(@Value("${xinfei.license.node-id:}") String explicitNodeId) {
        this.explicitNodeId = explicitNodeId == null ? "" : explicitNodeId.trim();
    }

    /** 本机指纹：64 位小写 SHA-256 hex，进程内缓存（因子不会在运行期变化）。 */
    public String get() {
        String f = fingerprint;
        if (f == null) {
            synchronized (this) {
                if (fingerprint == null) {
                    fingerprint = compute();
                }
                f = fingerprint;
            }
        }
        return f;
    }

    /**
     * 供页面展示/客户抄写的分段形式（每 8 位一组用 '-' 连接）。客户可整段复制发给作者，
     * 签发器会剥离所有非十六进制字符还原成 64 位指纹。
     */
    public String masked() {
        String hex = get();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hex.length(); i += 8) {
            if (i > 0) {
                sb.append('-');
            }
            sb.append(hex, i, Math.min(i + 8, hex.length()));
        }
        return sb.toString();
    }

    private String compute() {
        Map<String, String> factors = new LinkedHashMap<>();
        if (StringUtils.hasText(explicitNodeId)) {
            factors.put("nodeId", explicitNodeId);
        }
        String machineId = readFirstNonBlank(MACHINE_ID_FILES);
        if (machineId != null) {
            factors.put("machineId", machineId);
        }
        String productUuid = readTrim(PRODUCT_UUID);
        if (productUuid != null) {
            factors.put("productUuid", productUuid.toLowerCase());
        }
        if (factors.isEmpty()) {
            String mac = firstStableMac();
            if (mac != null) {
                factors.put("mac", mac);
                log.warn("[license] 未取到 machine-id/product_uuid，机器指纹回落网卡 MAC（容器重建可能变化）；"
                        + "生产环境建议在 compose 挂载宿主 /etc/machine-id 或配置 xinfei.license.node-id");
            }
        }
        if (factors.isEmpty()) {
            factors.put("random", UUID.randomUUID().toString().replace("-", ""));
            log.error("[license] 无法采集任何稳定机器因子，已回落随机指纹，机器绑定不稳定，请检查挂载/配置");
        }
        StringBuilder canonical = new StringBuilder();
        factors.forEach((k, v) -> canonical.append(k).append('=').append(v).append('\n'));
        String hex = sha256Hex(canonical.toString());
        log.info("[license] 机器指纹已生成：因子={}，指纹={}", String.join(",", factors.keySet()), hex);
        return hex;
    }

    private String readFirstNonBlank(String... files) {
        for (String f : files) {
            String v = readTrim(f);
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    private String readTrim(String file) {
        try {
            Path p = Path.of(file);
            if (Files.isRegularFile(p)) {
                String s = Files.readString(p, StandardCharsets.UTF_8).trim();
                return StringUtils.hasText(s) ? s : null;
            }
        } catch (Exception ignored) {
            // 不存在/无权限则尝试下一来源
        }
        return null;
    }

    private String firstStableMac() {
        try {
            Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
            while (nis != null && nis.hasMoreElements()) {
                NetworkInterface ni = nis.nextElement();
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) {
                    continue;
                }
                byte[] mac = ni.getHardwareAddress();
                if (mac == null || mac.length != 6) {
                    continue;
                }
                StringBuilder hex = new StringBuilder();
                boolean allZero = true;
                for (byte b : mac) {
                    int v = b & 0xFF;
                    if (v != 0) {
                        allZero = false;
                    }
                    hex.append(HexFormat.of().toHexDigits((long) v, 2));
                }
                if (!allZero) {
                    return hex.toString();
                }
            }
        } catch (Exception ignored) {
            // 回落随机
        }
        return null;
    }

    private static String sha256Hex(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
