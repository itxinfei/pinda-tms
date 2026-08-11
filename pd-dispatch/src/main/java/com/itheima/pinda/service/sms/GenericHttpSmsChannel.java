package com.itheima.pinda.service.sms;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * 通用 HTTP 短信网关渠道
 *
 * <p>适配自建短信网关或第三方标准 HTTP 短信接口：
 * 以 POST JSON 推送 {@code {mobile, content, time}}，网关负责真实下发。
 * 这是默认可用的通用通道（对接各类短信平台聚合网关）。</p>
 */
@Slf4j
@Component
public class GenericHttpSmsChannel implements SmsChannel {

    /**
     * 网关地址（可选，未配置时视为未启用）
     */
    @Value("${sms.channel.http.url:${sms.webhook-url:}}")
    private String httpUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String channelCode() {
        return "http";
    }

    @Override
    public boolean sendSms(String mobile, String content) {
        if (httpUrl == null || httpUrl.trim().isEmpty()) {
            log.info("[短信渠道-HTTP] 未配置网关地址，跳过");
            return false;
        }
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("mobile", mobile);
            payload.put("content", content);
            payload.put("time", java.time.LocalDateTime.now().toString());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(JSON.toJSONString(payload), headers);
            restTemplate.postForEntity(httpUrl, entity, String.class);
            log.info("[短信渠道-HTTP] 推送成功: mobile={}", mobile);
            return true;
        } catch (Exception e) {
            log.warn("[短信渠道-HTTP] 推送失败: mobile={}", mobile, e);
            return false;
        }
    }
}
