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
 * 通用设备短信接口渠道
 *
 * <p>适配短信猫/短信网关设备等通用设备接口（HTTP JSON 协议）：
 * 设备地址配置为 {@code sms.channel.device.url} 时，以 POST JSON 推送
 * {@code {mobile, content, timestamp}}，由设备侧完成真实短信下发。
 * 适用于自建短信硬件网关、短信池等场景。</p>
 */
@Slf4j
@Component
public class DeviceSmsChannel implements SmsChannel {

    /**
     * 通用设备短信接口地址（可选，未配置时视为未启用）
     */
    @Value("${sms.channel.device.url:}")
    private String deviceUrl;

    /**
     * 设备鉴权令牌（可选）
     */
    @Value("${sms.channel.device.token:}")
    private String deviceToken;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String channelCode() {
        return "device";
    }

    @Override
    public boolean sendSms(String mobile, String content) {
        if (deviceUrl == null || deviceUrl.trim().isEmpty()) {
            log.info("[短信渠道-设备] 未配置设备接口地址，跳过");
            return false;
        }
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("mobile", mobile);
            payload.put("content", content);
            payload.put("timestamp", System.currentTimeMillis());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (deviceToken != null && !deviceToken.trim().isEmpty()) {
                headers.set("Authorization", "Bearer " + deviceToken);
            }
            HttpEntity<String> entity = new HttpEntity<>(JSON.toJSONString(payload), headers);
            restTemplate.postForEntity(deviceUrl, entity, String.class);
            log.info("[短信渠道-设备] 设备下发成功: mobile={}", mobile);
            return true;
        } catch (Exception e) {
            log.warn("[短信渠道-设备] 设备下发失败: mobile={}", mobile, e);
            return false;
        }
    }
}
