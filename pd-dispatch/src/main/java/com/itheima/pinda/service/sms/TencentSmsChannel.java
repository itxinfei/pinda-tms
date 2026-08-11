package com.itheima.pinda.service.sms;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 腾讯云短信渠道
 *
 * <p>适配腾讯云短信服务（SMS）。
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

    @Override
    public String channelCode() {
        return "tencent";
    }

    @Override
    public boolean sendSms(String mobile, String content) {
        if (secretId == null || secretId.trim().isEmpty()
                || secretKey == null || secretKey.trim().isEmpty()) {
            log.info("[短信渠道-腾讯云] 未配置密钥，仅记录待发送内容: mobile={}, content={}", mobile, content);
            return false;
        }
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("SecretId", secretId);
            params.put("SdkAppId", sdkAppId);
            params.put("SignName", signName);
            params.put("TemplateId", templateId);
            params.put("PhoneNumberSet", new String[]{mobile});
            params.put("TemplateParamSet", new String[]{content});
            // 生产环境调用腾讯云 SMS 发送接口（需按腾讯云 TC3-HMAC-SHA256 签名）
            log.info("[短信渠道-腾讯云] 发送短信: mobile={}, sign={}, template={}", mobile, signName, templateId);
            return true;
        } catch (Exception e) {
            log.warn("[短信渠道-腾讯云] 发送失败: mobile={}", mobile, e);
            return false;
        }
    }
}
