package com.itheima.pinda.service.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * 腾讯云短信渠道
 *
 * <p>适配腾讯云短信服务（SMS），按腾讯云 TC3-HMAC-SHA256 签名规范调用发送接口。
 * 通过配置注入 SecretId/SecretKey/签名/模板；未配置时降级为日志记录，保证流程可运行。</p>
 */
@Slf4j
@Component
public class TencentSmsChannel implements SmsChannel {

    /**
     * 腾讯云 SecretId
     */
    @Value("${sms.channel.tencent.secret-id:}")
    private String secretId;

    /**
     * 腾讯云 SecretKey
     */
    @Value("${sms.channel.tencent.secret-key:}")
    private String secretKey;

    /**
     * 短信应用ID
     */
    @Value("${sms.channel.tencent.sdk-app-id:}")
    private String sdkAppId;

    /**
     * 短信签名
     */
    @Value("${sms.channel.tencent.sign-name:品达物流}")
    private String signName;

    /**
     * 短信模板ID
     */
    @Value("${sms.channel.tencent.template-id:}")
    private String templateId;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String channelCode() {
        return "tencent";
    }

    @Override
    public boolean sendSms(String mobile, String content) {
        if (secretId == null || secretId.trim().isEmpty()
                || secretKey == null || secretKey.trim().isEmpty()) {
            log.info("[短信渠道-腾讯云] 未配置密钥，仅记录待发送手机号: mobile={}", maskMobile(mobile));
            return false;
        }
        try {
            // 腾讯云 TC3-HMAC-SHA256 签名（SMS SendSms）
            String service = "sms";
            String host = "sms.tencentcloudapi.com";
            String action = "SendSms";
            String version = "2021-01-11";
            String algorithm = "TC3-HMAC-SHA256";
            java.time.ZonedDateTime now = java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC);
            String timestamp = String.valueOf(now.toEpochSecond());
            String date = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd").format(now);

            String payload = "{\"PhoneNumberSet\":[\"" + mobile + "\"],"
                + "\"SmsSdkAppId\":\"" + sdkAppId + "\","
                + "\"SignName\":\"" + signName + "\","
                + "\"TemplateId\":\"" + templateId + "\","
                + "\"TemplateParamSet\":[\"" + content + "\"]}";

            // 1. 拼接规范请求串
            String canonicalHeaders = "content-type:application/json; charset=utf-8\nhost:" + host + "\nx-tc-action:" + action.toLowerCase() + "\n";
            String canonicalRequest = "POST\n/\n\n" + canonicalHeaders + "\ncontent-type;host;x-tc-action\n" + SmsSignUtils.sha256Hex(payload);
            // 2. 拼接待签名字符串
            String credentialScope = date + "/" + service + "/tc3_request";
            String stringToSign = algorithm + "\n" + timestamp + "\n" + credentialScope + "\n" + SmsSignUtils.sha256Hex(canonicalRequest);
            // 3. 派生签名密钥并签名
            String secretDate = SmsSignUtils.hmacSha256Hex("TC3" + secretKey, date);
            String secretService = SmsSignUtils.hmacSha256Hex(secretDate, service);
            String secretSigning = SmsSignUtils.hmacSha256Hex(secretService, "tc3_request");
            String signature = SmsSignUtils.hmacSha256Hex(secretSigning, stringToSign);
            String authorization = algorithm + " Credential=" + secretId + "/" + credentialScope
                + ", SignedHeaders=content-type;host;x-tc-action, Signature=" + signature;

            // 4. 发起请求
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            headers.set("Authorization", authorization);
            headers.set("X-TC-Action", action);
            headers.set("X-TC-Timestamp", timestamp);
            headers.set("X-TC-Version", version);
            headers.set("X-TC-Region", "ap-guangzhou");
            org.springframework.http.HttpEntity<String> entity =
                new org.springframework.http.HttpEntity<>(payload, headers);
            String resp = restTemplate.postForObject("https://" + host, entity, String.class);
            log.info("[短信渠道-腾讯云] 发送短信: mobile={}, sign={}, template={}, resp={}",
                maskMobile(mobile), signName, templateId, resp);
            return resp != null && resp.contains("\"Code\":\"Ok\"");
        } catch (Exception e) {
            log.warn("[短信渠道-腾讯云] 发送失败: mobile={}", maskMobile(mobile), e);
            return false;
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
