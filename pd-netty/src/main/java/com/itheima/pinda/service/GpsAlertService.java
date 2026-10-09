package com.itheima.pinda.service;

import com.alibaba.fastjson.JSON;
import com.itheima.pinda.entity.AlarmRecord;
import com.itheima.pinda.entity.LocationEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
 *   <li><b>落库</b>：写入 pd_alarm_record（best effort，失败仅 log，不影响 GPS 主消费链路；
 *       同一运输任务 + 告警类型存在未处理记录时不重复插入）；</li>
 *   <li><b>HTTP Webhook</b>：配置 {@code gps.alert.webhook-url} 后，将告警推送到外部通知网关
 *       （可对接钉钉/企业微信/自建通知中心，再由网关转发邮件/短信）。</li>
 * </ol>
 * 未配置 Webhook 时仅记录日志与落库，不影响主流程。</p>
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

    /**
     * 告警落库 Service
     */
    @Autowired
    private IAlarmRecordService alarmRecordService;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 发出告警（带轨迹上下文，推荐使用）
     *
     * @param alertType 告警类型（SPEED_OVER、STAY_TOO_LONG、DEVIATE_ROUTE）
     * @param location  触发告警的轨迹点（提供运输任务、车辆/司机、经纬度上下文）
     * @param message   告警内容
     */
    public void alert(String alertType, LocationEntity location, String message) {
        String businessId = location == null ? null : location.getBusinessId();

        // 1. 日志告警（始终记录）
        log.error("[GPS告警] type={}, businessId={}, message={}", alertType, businessId, message);

        // 2. 落库告警（best effort：失败只 log，不影响 GPS 主消费链路）
        persistAlarm(alertType, location, message);

        if (!alertEnabled) {
            return;
        }

        // 3. HTTP Webhook 推送（可选）
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

    /**
     * 发出告警（无轨迹上下文，兼容旧调用）
     *
     * @param alertType  告警类型（如 SPEED_OVER、STAY_TOO_LONG）
     * @param businessId 业务ID（车辆/快递员ID）
     * @param message    告警内容
     */
    public void alert(String alertType, String businessId, String message) {
        alert(alertType, buildSimpleContext(businessId), message);
    }

    /**
     * 将仅有 businessId 的旧调用包装为轨迹上下文
     *
     * @param businessId 业务ID
     * @return 最小上下文（businessId 同时作为车辆/司机id，类型未知时不区分）
     */
    private LocationEntity buildSimpleContext(String businessId) {
        if (businessId == null || businessId.trim().isEmpty()) {
            return null;
        }
        LocationEntity location = new LocationEntity();
        location.setBusinessId(businessId);
        return location;
    }

    /**
     * 告警落库：组装 AlarmRecord 并走幂等写入，任何异常都只 log 不抛出
     *
     * @param alertType 告警类型
     * @param location  轨迹点上下文（可为 null）
     * @param message   告警内容
     */
    private void persistAlarm(String alertType, LocationEntity location, String message) {
        try {
            LocalDateTime now = LocalDateTime.now();
            AlarmRecord record = new AlarmRecord();
            record.setAlarmType(alertType);
            record.setAlarmContent(truncate(message, 500));
            record.setAlarmTime(now);
            record.setStatus(0);
            record.setCreateTime(now);
            if (location != null) {
                record.setTransportTaskId(location.getTransportTaskId());
                // businessId 按上报类型区分车辆/司机；类型缺失时优先按车辆记录
                if ("courier".equalsIgnoreCase(location.getType())) {
                    record.setDriverId(location.getBusinessId());
                } else {
                    record.setTruckId(location.getBusinessId());
                }
                record.setLongitude(parseDouble(location.getLng()));
                record.setLatitude(parseDouble(location.getLat()));
            }
            boolean inserted = alarmRecordService.saveIfNotDuplicate(record);
            if (inserted) {
                log.info("[GPS告警] 告警已落库: id={}, type={}", record.getId(), alertType);
            }
        } catch (Exception e) {
            // 落库失败不影响 GPS 主消费链路，仅记录
            log.warn("[GPS告警] 告警落库失败（已降级，不影响主流程）: type={}", alertType, e);
        }
    }

    /**
     * 安全解析 double，失败或入参为空时返回 null
     *
     * @param value 数字字符串
     * @return Double 值或 null
     */
    private Double parseDouble(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 按最大长度截断字符串，避免超过列长度导致整条告警落库失败
     *
     * @param value    原始字符串
     * @param maxLength 最大长度
     * @return 截断后的字符串
     */
    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
