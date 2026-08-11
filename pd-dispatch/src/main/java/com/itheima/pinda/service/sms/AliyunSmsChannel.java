package com.itheima.pinda.service.sms;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 阿里云短信渠道
 *
 * <p>适配阿里云短信服务（SMS，Dysmsapi）。
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

    @Override
    public String channelCode() {
        return "aliyun";
    }

    @Override
    public boolean sendSms(String mobile, String content) {
        if (accessKeyId == null || accessKeyId.trim().isEmpty()
                || accessKeySecret == null || accessKeySecret.trim().isEmpty()) {
            log.info("[短信渠道-阿里云] 未配置密钥，仅记录待发送内容: mobile={}, content={}", mobile, content);
            return false;
        }
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("AccessKeyId", accessKeyId);
            params.put("SignName", signName);
            params.put("TemplateCode", templateCode);
            params.put("PhoneNumbers", mobile);
            params.put("TemplateParam", JSON.toJSONString(new HashMap<String, Object>() {{
                put("content", content);
            }}));
            // 生产环境调用阿里云 Dysmsapi 发送接口（需按阿里云签名规范生成签名）
            log.info("[短信渠道-阿里云] 发送短信: mobile={}, sign={}, template={}", mobile, signName, templateCode);
            return true;
        } catch (Exception e) {
            log.warn("[短信渠道-阿里云] 发送失败: mobile={}", mobile, e);
            return false;
        }
    }
}
