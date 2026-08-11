package com.itheima.pinda.service;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 短信/通知服务
 *
 * <p>订单确认、揽收完成、交付(签收/拒收)等业务事件统一通过本组件向客户发送通知。
 * 通知通道（可扩展）：
 * <ol>
 *   <li><b>日志</b>：始终记录通知内容（含手机号脱敏处理）；</li>
 *   <li><b>短信网关 Webhook</b>：配置 {@code sms.webhook-url} 后，将通知推送到外部短信网关
 *       （可对接阿里云/腾讯云短信等，再由网关发送真实短信）。</li>
 * </ol>
 * 未配置 Webhook 时仅记录日志，不影响业务主流程。</p>
 */
@Slf4j
@Component
public class SmsNotificationService {

    /**
     * 短信网关 Webhook 地址（可选，配置后启用推送）
     */
    @Value("${sms.webhook-url:}")
    private String smsWebhookUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 发送短信通知
     *
     * @param mobile 手机号
     * @param content 短信内容
     */
    public void sendSms(String mobile, String content) {
        // 1. 日志记录（手机号脱敏）
        log.info("[短信通知] 收件人: {}, 内容: {}", maskMobile(mobile), content);

        if (smsWebhookUrl == null || smsWebhookUrl.trim().isEmpty()) {
            log.info("[短信通知] 未配置短信网关(sms.webhook-url)，仅记录日志");
            return;
        }

        // 2. 推送到短信网关
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("mobile", mobile);
            payload.put("content", content);
            payload.put("time", LocalDateTime.now().toString());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(JSON.toJSONString(payload), headers);
            restTemplate.postForEntity(smsWebhookUrl, entity, String.class);
            log.info("[短信通知] 短信网关推送成功: mobile={}", maskMobile(mobile));
        } catch (Exception e) {
            log.warn("[短信通知] 短信网关推送失败: mobile={}", maskMobile(mobile), e);
        }
    }

    /**
     * 手机号脱敏：保留前3后4，中间用*号
     * （包内可见，便于单元测试验证脱敏规则）
     *
     * @param mobile 手机号
     * @return 脱敏后手机号
     */
    String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 7) {
            return "****";
        }
        return mobile.substring(0, 3) + "****" + mobile.substring(mobile.length() - 4);
    }
}
