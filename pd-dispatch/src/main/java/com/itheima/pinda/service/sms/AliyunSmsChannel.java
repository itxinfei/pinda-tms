package com.itheima.pinda.service.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * 阿里云短信渠道
 *
 * <p>适配阿里云短信服务（SMS，Dysmsapi），按阿里云 RPC 签名规范生成签名后调用发送接口。
 * 通过配置注入 AccessKey/签名/模板；未配置时降级为日志记录，保证流程可运行。</p>
 */
@Slf4j
@Component
public class AliyunSmsChannel implements SmsChannel {

    /**
     * 阿里云 AccessKeyId
     */
    @Value("${sms.channel.aliyun.access-key-id:}")
    private String accessKeyId;

    /**
     * 阿里云 AccessKeySecret
     */
    @Value("${sms.channel.aliyun.access-key-secret:}")
    private String accessKeySecret;

    /**
     * 短信签名
     */
    @Value("${sms.channel.aliyun.sign-name:品达物流}")
    private String signName;

    /**
     * 短信模板编码
     */
    @Value("${sms.channel.aliyun.template-code:}")
    private String templateCode;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String channelCode() {
        return "aliyun";
    }

    @Override
    public boolean sendSms(String mobile, String content) {
        if (accessKeyId == null || accessKeyId.trim().isEmpty()
                || accessKeySecret == null || accessKeySecret.trim().isEmpty()) {
            log.info("[短信渠道-阿里云] 未配置密钥，仅记录待发送手机号: mobile={}", maskMobile(mobile));
            return false;
        }
        try {
            // 阿里云 RPC 签名（Dysmsapi）：公共参数 + 业务参数按字典序拼接后 HMAC-SHA1 签名
            java.util.TreeMap<String, String> params = new java.util.TreeMap<>();
            params.put("AccessKeyId", accessKeyId);
            params.put("Action", "SendSms");
            params.put("Version", "2017-05-25");
            params.put("Format", "JSON");
            params.put("RegionId", "cn-hangzhou");
            params.put("SignatureMethod", "HMAC-SHA1");
            params.put("SignatureVersion", "1.0");
            params.put("SignatureNonce", String.valueOf(System.nanoTime()));
            params.put("Timestamp", java.time.format.DateTimeFormatter
                .ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
                .format(java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC)));
            params.put("PhoneNumbers", mobile);
            params.put("SignName", signName);
            params.put("TemplateCode", templateCode);
            params.put("TemplateParam", "{\"content\":\"" + content + "\"}");

            // 构造待签名字符串: GET&%2F&(urlencoded 参数按字典序拼接)
            StringBuilder canonical = new StringBuilder();
            params.forEach((k, v) -> {
                if (canonical.length() > 0) {
                    canonical.append("&");
                }
                canonical.append(SmsSignUtils.percentEncode(k)).append("=").append(SmsSignUtils.percentEncode(v));
            });
            String stringToSign = "GET&%2F&" + SmsSignUtils.percentEncode(canonical.toString());
            // RPC 签名使用 HMAC-SHA1，密钥为 AccessKeySecret + "&"
            String signature = hmacSha1Base64(accessKeySecret + "&", stringToSign);
            params.put("Signature", signature);

            // 拼接查询串并发起请求
            StringBuilder query = new StringBuilder();
            params.forEach((k, v) -> {
                if (query.length() > 0) {
                    query.append("&");
                }
                query.append(SmsSignUtils.percentEncode(k)).append("=").append(SmsSignUtils.percentEncode(v));
            });
            String url = "https://dysmsapi.aliyuncs.com/?" + query;
            String resp = restTemplate.getForObject(url, String.class);
            log.info("[短信渠道-阿里云] 发送短信: mobile={}, sign={}, template={}, resp={}",
                maskMobile(mobile), signName, templateCode, resp);
            return resp != null && resp.contains("\"Code\":\"OK\"");
        } catch (Exception e) {
            log.warn("[短信渠道-阿里云] 发送失败: mobile={}", maskMobile(mobile), e);
            return false;
        }
    }

    /**
     * HMAC-SHA1 Base64（阿里云 RPC 签名）
     */
    private String hmacSha1Base64(String secret, String data) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA1");
            mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA1"));
            return java.util.Base64.getEncoder().encodeToString(mac.doFinal(data.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA1 计算失败", e);
        }
    }

    /**
     * 手机号脱敏：保留前3后4
     */
    private String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 7) {
            return "****";
        }
        return mobile.substring(0, 3) + "****" + mobile.substring(mobile.length() - 4);
    }
}
