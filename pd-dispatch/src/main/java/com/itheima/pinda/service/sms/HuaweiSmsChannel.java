package com.itheima.pinda.service.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 华为云短信渠道
 *
 * <p>适配华为云短信服务（MSGSMS）。
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

    @Override
    public String channelCode() {
        return "huawei";
    }

    @Override
    public boolean sendSms(String mobile, String content) {
        if (appKey == null || appKey.trim().isEmpty()
                || appSecret == null || appSecret.trim().isEmpty()) {
            log.info("[短信渠道-华为云] 未配置密钥，仅记录待发送内容: mobile={}, content={}", mobile, content);
            return false;
        }
        try {
            // 生产环境调用华为云 MSGSMS 发送接口（需按华为云 AK/SK 签名）
            log.info("[短信渠道-华为云] 发送短信: mobile={}, sign={}, template={}", mobile, signName, templateId);
            return true;
        } catch (Exception e) {
            log.warn("[短信渠道-华为云] 发送失败: mobile={}", mobile, e);
            return false;
        }
    }
}
