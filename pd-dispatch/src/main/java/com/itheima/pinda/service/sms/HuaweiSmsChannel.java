package com.itheima.pinda.service.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * 华为云短信渠道
 *
 * <p>适配华为云短信服务（MSGSMS），按华为云 AK/SK 签名规范（HMAC-SHA256）调用发送接口。
 * 通过配置注入 AppKey/AppSecret/签名/模板；未配置时降级为日志记录，保证流程可运行。</p>
 */
@Slf4j
@Component
public class HuaweiSmsChannel implements SmsChannel {

    /**
     * 华为云 AppKey
     */
    @Value("${sms.channel.huawei.app-key:}")
    private String appKey;

    /**
     * 华为云 AppSecret
     */
    @Value("${sms.channel.huawei.app-secret:}")
    private String appSecret;

    /**
     * 短信签名
     */
    @Value("${sms.channel.huawei.sign-name:品达物流}")
    private String signName;

    /**
     * 短信模板ID
     */
    @Value("${sms.channel.huawei.template-id:}")
    private String templateId;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String channelCode() {
        return "huawei";
    }

    @Override
    public boolean sendSms(String mobile, String content) {
        if (appKey == null || appKey.trim().isEmpty()
                || appSecret == null || appSecret.trim().isEmpty()) {
            log.info("[短信渠道-华为云] 未配置密钥，仅记录待发送手机号: mobile={}", maskMobile(mobile));
            return false;
        }
        try {
            // 华为云 MSGSMS：POST form 表单 + Authorization 头（AK/SK HMAC-SHA256 签名）
            String url = "https://smsapi.cn-north-4.myhuaweicloud.com:443/sms/batchSendSms/v1";
            String templateParas = "[\"" + content + "\"]";
            // 1. 拼接待签字符串: appKey + "\n" + mobile + "\n" + templateId + "\n" + templateParas + "\n" + signName + "\n"
            String stringToSign = appKey + "\n" + mobile + "\n" + templateId + "\n" + templateParas + "\n" + signName + "\n";
            String signature = SmsSignUtils.hmacSha256Base64(appSecret, stringToSign);

            // 2. 构造 form 表单
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Authorization", "WSSE realm=\"SDP\",profile=\"UsernameToken\",type=\"Appkey\"");
            headers.set("X-WSSE", buildWsse());

            StringBuilder form = new StringBuilder();
            form.append("from=").append(signName)
                .append("&to=").append(mobile)
                .append("&templateId=").append(templateId)
                .append("&templateParas=").append(java.net.URLEncoder.encode(templateParas, "UTF-8"))
                .append("&signature=").append(java.net.URLEncoder.encode(signature, "UTF-8"));
            HttpEntity<String> entity = new HttpEntity<>(form.toString(), headers);
            String resp = restTemplate.postForObject(url, entity, String.class);
            log.info("[短信渠道-华为云] 发送短信: mobile={}, sign={}, template={}, resp={}",
                maskMobile(mobile), signName, templateId, resp);
            // 华为云返回体 code 字段，000000 表示成功
            return resp != null && resp.contains("\"code\":\"000000\"");
        } catch (Exception e) {
            log.warn("[短信渠道-华为云] 发送失败: mobile={}", maskMobile(mobile), e);
            return false;
        }
    }

    /**
     * 构建 WSSE 认证头（华为云规范：时间戳+随机数，用 AppSecret 做 HMAC-SHA256）
     */
    private String buildWsse() {
        try {
            String time = java.time.format.DateTimeFormatter
                .ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
                .format(java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC));
            String nonce = java.util.UUID.randomUUID().toString().replace("-", "");
            String passwordDigest = SmsSignUtils.hmacSha256Base64(appSecret, time + nonce);
            return "UsernameToken Username=\"" + appKey + "\",PasswordDigest=\""
                + passwordDigest + "\",Nonce=\"" + nonce + "\",Created=\"" + time + "\"";
        } catch (Exception e) {
            throw new IllegalStateException("构建 WSSE 头失败", e);
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
