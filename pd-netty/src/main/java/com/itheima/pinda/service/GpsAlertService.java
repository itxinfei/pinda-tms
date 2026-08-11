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
 * GPS 异常告警服务
 *
 * <p>超速、长时间停留等异常检测结果统一通过本组件发出告警。
 * 告警通道（可扩展）：
 * <ol>
 *   <li><b>日志</b>：始终记录 error 级别告警日志；</li>
 *   <li><b>HTTP Webhook</b>：配置 {@code gps.alert.webhook-url} 后，将告警推送到外部通知网关
 *       （可对接钉钉/企业微信/自建通知中心，再由网关转发邮件/短信）。</li>
 * </ol>
 * 未配置 Webhook 时仅记录日志，不影响主流程。</p>
 */
@Slf4j
@Component
public class GpsAlertService {

    /**
     * 告警开关（默认开启）
     */
    @Value("${gps.alert.enabled:true}")
    private boolean alertEnabled;

    /**
     * Webhook 通知地址（可选，配置后启用 HTTP 推送）
     */
    @Value("${gps.alert.webhook-url:}")
    private String webhookUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 发出告警
     *
     * @param alertType  告警类型（如 SPEED_OVER、STAY_TOO_LONG）
     * @param businessId 业务ID（车辆/快递员ID）
     * @param message    告警内容
     */
    public void alert(String alertType, String businessId, String message) {
        // 1. 日志告警（始终记录）
        log.error("[GPS告警] type={}, businessId={}, message={}", alertType, businessId, message);

        if (!alertEnabled) {
            return;
        }

        // 2. HTTP Webhook 推送（可选）
        if (webhookUrl != null && !webhookUrl.trim().isEmpty()) {
            try {
                Map<String, Object> payload = new HashMap<>();
                payload.put("alertType", alertType);
                payload.put("businessId", businessId);
                payload.put("message", message);
                payload.put("time", LocalDateTime.now().toString());

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<String> entity = new HttpEntity<>(JSON.toJSONString(payload), headers);
                restTemplate.postForEntity(webhookUrl, entity, String.class);
                log.info("[GPS告警] Webhook 推送成功: type={}, businessId={}", alertType, businessId);
            } catch (Exception e) {
                // Webhook 推送失败不影响主流程，仅记录告警
                log.warn("[GPS告警] Webhook 推送失败: type={}, businessId={}", alertType, businessId, e);
            }
        }
    }
}
