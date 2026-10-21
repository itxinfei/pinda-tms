package com.xinfei.auth.license;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Base64;

/**
 * 授权码签发工具（<b>作者侧</b>，仅本地运行，绝不入库）。
 *
 * <p>用法：
 * <pre>
 *   java -cp xfwl-auth.jar com.xinfei.auth.license.LicenseGenerator \
 *        --licensee "某某物流有限公司" --type ENTERPRISE --days 365 --instances 1
 * </pre>
 * 私钥默认读取 {@code -Dlicense.private=...} 或参数 {@code --private}，
 * 形如 {@code /path/to/license-private.pem}。
 * 输出一串 license key，交给客户设置环境变量 {@code XINFEI_LICENSE}
 * 或写入 {@code license.key} 文件即可激活。</p>
 */
public final class LicenseGenerator {

    private LicenseGenerator() {
    }

    public static void main(String[] args) throws Exception {
        String privPath = System.getProperty("license.private", arg(args, "--private",
                "/d/MyCode/pinda-tms/.workbuddy/tmp/license-private.pem"));
        String pem = Files.readString(Path.of(privPath));
        PrivateKey key = PemUtils.privateKey(pem);

        String licensee = arg(args, "--licensee", "心飞TMS演示客户");
        String type = arg(args, "--type", "ENTERPRISE");
        int days = Integer.parseInt(arg(args, "--days", "365"));
        int instances = Integer.parseInt(arg(args, "--instances", "1"));

        ObjectMapper m = new JsonMapper();
        ObjectNode p = m.createObjectNode();
        p.put("licensee", licensee);
        p.put("type", type);
        p.put("instances", instances);
        p.put("issuedAt", System.currentTimeMillis());
        p.put("expiresAt", System.currentTimeMillis() + (long) days * 86_400_000L);

        String payloadB64 = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(m.writeValueAsBytes(p));

        Signature s = Signature.getInstance("SHA256withRSA");
        s.initSign(key);
        s.update(payloadB64.getBytes(StandardCharsets.US_ASCII));
        String sigB64 = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(s.sign());

        String licenseKey = payloadB64 + "." + sigB64;
        System.out.println("==== 授权码（复制给客户，设置环境变量 XINFEI_LICENSE 或写入 license.key）====");
        System.out.println(licenseKey);
        System.out.println("==================================================================================");
    }

    private static String arg(String[] args, String name, String def) {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals(name)) {
                return args[i + 1];
            }
        }
        return def;
    }
}
