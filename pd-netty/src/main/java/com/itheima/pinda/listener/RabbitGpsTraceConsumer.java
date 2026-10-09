package com.itheima.pinda.listener;

import com.alibaba.fastjson.JSON;
import com.itheima.pinda.client.TruckBaseClient;
import com.itheima.pinda.entity.LocationEntity;
import com.itheima.pinda.entity.LocationRecord;
import com.itheima.pinda.enums.CoordSystem;
import com.itheima.pinda.enums.LocationSource;
import com.itheima.pinda.service.GpsAlertService;
import com.itheima.pinda.service.ILocationRecordService;
import com.itheima.pinda.utils.CoordTransform;
import com.itheima.pinda.utils.GpsLocationValidator;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.Acknowledgment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * GPS轨迹数据RabbitMQ消费端（替代原Kafka版本）
 *
 * 处理逻辑:
 * 1. 接收GPS位置数据
 * 2. 坐标统一转换为 BD09
 * 3. 存储轨迹点
 * 4. 异常检测（超速、偏离路线、长时间停留）
 * 5. 车辆心跳（在线/离线闭环）
 *
 * @author Claude Code
 * @since 2026-10-09
 */
@Slf4j
@Component
public class RabbitGpsTraceConsumer {

    @Autowired
    private ILocationRecordService locationRecordService;

    @Autowired
    private GpsAlertService gpsAlertService;

    @Autowired
    private TruckBaseClient truckBaseClient;

    /**
     * 轨迹点内存缓存（按车辆/快递员ID分组）
     */
    private static final int MAX_CACHE_KEYS = 5000;
    private static final Map<String, List<LocationEntity>> TRACE_CACHE =
            Collections.synchronizedMap(new LinkedHashMap<String, List<LocationEntity>>(MAX_CACHE_KEYS, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, List<LocationEntity>> eldest) {
                    return size() > MAX_CACHE_KEYS;
                }
            });

    private static final int MAX_CACHE_SIZE = 1000;
    private static final AtomicLong TRACE_COUNT = new AtomicLong(0);
    private static final long CACHE_EXPIRE_MINUTES = 60;

    private static final ScheduledExecutorService CLEANUP_EXECUTOR = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "gps-trace-cache-cleanup");
        t.setDaemon(true);
        return t;
    });

    @Value("${gps.trace.retention-days:30}")
    private int retentionDays;

    static {
        CLEANUP_EXECUTOR.scheduleAtFixedRate(RabbitGpsTraceConsumer::cleanupExpiredCache, 1, 1, TimeUnit.HOURS);
    }

    private static void cleanupExpiredCache() {
        try {
            RabbitGpsTraceConsumer consumer = com.itheima.pinda.common.utils.SpringContextUtils.getBean(RabbitGpsTraceConsumer.class);
            consumer.doCleanup();
        } catch (Exception e) {
            log.error("[GPS清理] 定时清理任务执行失败", e);
        }
    }

    private void doCleanup() {
        long expireThreshold = System.currentTimeMillis() - CACHE_EXPIRE_MINUTES * 60 * 1000;
        TRACE_CACHE.keySet().removeIf(key -> {
            List<LocationEntity> points = TRACE_CACHE.get(key);
            if (points == null || points.isEmpty()) {
                return true;
            }
            LocationEntity last = points.get(points.size() - 1);
            try {
                long lastTime = LocalDateTime.parse(last.getCurrentTime(), TIME_FORMATTER)
                    .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                return lastTime < expireThreshold;
            } catch (Exception e) {
                return false;
            }
        });
        log.info("[GPS清理] 内存缓存清理完成，剩余业务对象数: {}", TRACE_CACHE.size());

        if (retentionDays > 0) {
            try {
                int deleted = locationRecordService.cleanExpiredTraces(retentionDays);
                if (deleted > 0) {
                    log.info("[GPS清理] 数据库过期轨迹清理完成，删除 {} 条", deleted);
                }
            } catch (Exception e) {
                log.error("[GPS清理] 数据库过期轨迹清理失败", e);
            }
        }
    }

    @javax.annotation.PreDestroy
    public void destroy() {
        CLEANUP_EXECUTOR.shutdown();
    }

    private static final double SPEED_LIMIT = 120.0;
    private static final long STAY_THRESHOLD_MINUTES = 30;
    private static final int STAY_CHECK_WINDOW = 10;
    private static final double STAY_POSITION_THRESHOLD = 0.001;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final double GEO_FENCE_DEVIATE_KM = 2.0;
    private static final long HEARTBEAT_TIMEOUT_MINUTES = 5;

    /**
     * 消费GPS轨迹数据
     * 用手动ack，处理成功才ack，失败就nack并重新入队
     */
    @RabbitListener(queues = "pinda.gps.queue")
    public void consumeGpsTrace(String message, Channel channel, org.springframework.amqp.support.Acknowledgment ack) {
        try {
            LocationEntity location = JSON.parseObject(message, LocationEntity.class);
            LocationRecord locationRecord = buildLocationRecord(location);
            if (locationRecord != null) {
                locationRecordService.save(locationRecord);
            }
            ack.acknowledge();
            log.debug("[GPS落库] 单条写入成功并ack: businessId={}", location.getBusinessId());
        } catch (Exception e) {
            log.error("[GPS落库] 消息处理失败，nack并重新入队: msg={}", message, e);
            try {
                ack.nack(false, true);
            } catch (Exception ex) {
                log.error("[GPS落库] nack失败", ex);
            }
        }
    }

    private LocationRecord buildLocationRecord(LocationEntity location) {
        String invalidMsg = GpsLocationValidator.validateRequired(location);
        if (invalidMsg != null) {
            log.warn("[GPS消费] 轨迹点校验未通过被丢弃: msg={}, raw={}", invalidMsg, JSON.toJSONString(location));
            return null;
        }

        String coordSystem = location.getCoordSystem();
        if (!StringUtils.hasText(coordSystem)) {
            location.setCoordSystem(CoordSystem.BD09.getCode());
        } else if (!CoordSystem.isValid(coordSystem)) {
            log.warn("[GPS消费] 坐标系非法被丢弃: businessId={}, coordSystem={}", location.getBusinessId(), coordSystem);
            return null;
        }

        String source = location.getSource();
        if (!StringUtils.hasText(source)) {
            location.setSource(LocationSource.MOBILE.getCode());
        } else if (!LocationSource.isValid(source)) {
            log.warn("[GPS消费] 来源非法被丢弃: businessId={}, source={}", location.getBusinessId(), source);
            return null;
        }

        double lng = Double.parseDouble(location.getLng().trim());
        double lat = Double.parseDouble(location.getLat().trim());
        CoordSystem cs = CoordSystem.of(location.getCoordSystem());
        double[] bd09;
        if (cs == CoordSystem.BD09) {
            bd09 = new double[]{lng, lat};
        } else if (cs == CoordSystem.GCJ02) {
            bd09 = CoordTransform.gcj02ToBd09(lng, lat);
        } else {
            bd09 = CoordTransform.wgs84ToBd09(lng, lat);
        }
        location.setLng(CoordTransform.toFixed6(bd09[0]));
        location.setLat(CoordTransform.toFixed6(bd09[1]));
        location.setCoordSystem(CoordSystem.BD09.getCode());

        String cacheKey = location.getBusinessId() + "#" + location.getType();
        List<LocationEntity> tracePoints = TRACE_CACHE.computeIfAbsent(cacheKey, k ->
            Collections.synchronizedList(new LinkedList<>()));
        boolean sameAsLast;
        synchronized (tracePoints) {
            sameAsLast = !tracePoints.isEmpty() && isSameNaturalPoint(tracePoints.get(tracePoints.size() - 1), location);
            if (!sameAsLast) {
                tracePoints.add(location);
                if (tracePoints.size() > MAX_CACHE_SIZE) {
                    tracePoints.remove(0);
                }
            }
            long count = TRACE_COUNT.incrementAndGet();
            log.debug("[GPS消费] 轨迹点接收(BD09): businessId={}, lng={}, lat={}, source={}, 累计处理: {}",
                    location.getBusinessId(), location.getLng(), location.getLat(), location.getSource(), count);
            checkAnomalies(location, cacheKey, tracePoints);
        }

        if ("truck".equalsIgnoreCase(location.getType())) {
            boolean heartbeatOk = truckBaseClient.updateHeartbeat(location.getBusinessId(), LocalDateTime.now());
            if (!heartbeatOk) {
                log.warn("[GPS心跳] 车辆心跳更新失败（已降级，不影响轨迹落库）: businessId={}", location.getBusinessId());
            }
        }

        LocationRecord record = new LocationRecord();
        record.setId(location.getBusinessId() + "#" + location.getType() + "#" + location.getCurrentTime());
        record.setBusinessId(location.getBusinessId());
        record.setName(location.getName());
        record.setPhone(location.getPhone());
        record.setLicensePlate(location.getLicensePlate());
        record.setType(location.getType());
        record.setLng(location.getLng());
        record.setLat(location.getLat());
        record.setCurrentTime(location.getCurrentTime());
        record.setTeam(location.getTeam());
        record.setTransportTaskId(location.getTransportTaskId());
        record.setCreateTime(LocalDateTime.now());
        record.setCoordSystem(location.getCoordSystem());
        record.setSource(location.getSource());
        return record;
    }

    private boolean isSameNaturalPoint(LocationEntity a, LocationEntity b) {
        return a != null && b != null
                && java.util.Objects.equals(a.getBusinessId(), b.getBusinessId())
                && java.util.Objects.equals(a.getType(), b.getType())
                && java.util.Objects.equals(a.getCurrentTime(), b.getCurrentTime());
    }

    @Scheduled(fixedDelay = 60_000)
    public void scanOfflineTrucks() {
        try {
            LocalDateTime threshold = LocalDateTime.now().minusMinutes(HEARTBEAT_TIMEOUT_MINUTES);
            int affected = truckBaseClient.markOffline(threshold);
            if (affected < 0) {
                log.debug("[GPS心跳扫描] 调用 pd-base 失败（已降级）");
            } else if (affected > 0) {
                log.info("[GPS心跳扫描] 离线车辆更新: {} 辆", affected);
            }
        } catch (Exception e) {
            log.warn("[GPS心跳扫描] 扫描失败（已降级，不影响主流程）", e);
        }
    }

    private void checkAnomalies(LocationEntity location, String cacheKey, List<LocationEntity> tracePoints) {
        if (tracePoints.size() < 2) {
            return;
        }
        LocationEntity prevPoint = tracePoints.get(tracePoints.size() - 2);
        checkSpeed(location, prevPoint);
        checkStayTooLong(location, cacheKey, tracePoints);
        checkGeoFence(location, tracePoints);
    }

    private void checkSpeed(LocationEntity current, LocationEntity previous) {
        if (current.getLng() == null || current.getLat() == null
                || previous.getLng() == null || previous.getLat() == null) {
            return;
        }
        try {
            double currentLng = Double.parseDouble(current.getLng());
            double currentLat = Double.parseDouble(current.getLat());
            double prevLng = Double.parseDouble(previous.getLng());
            double prevLat = Double.parseDouble(previous.getLat());
            double distanceKm = calculateDistance(prevLat, prevLng, currentLat, currentLng);
            long timeDiffSeconds = parseTimeDiffSeconds(current.getCurrentTime(), previous.getCurrentTime());
            if (timeDiffSeconds > 0 && timeDiffSeconds < 300) {
                double speedKmh = (distanceKm / timeDiffSeconds) * 3600;
                if (speedKmh > SPEED_LIMIT) {
                    gpsAlertService.alert("SPEED_OVER", current,
                        String.format("超速提醒: 速度=%.1fkm/h, 阈值=%.0fkm/h, 位置=(%s, %s)",
                            speedKmh, SPEED_LIMIT, current.getLng(), current.getLat()));
                }
            }
        } catch (NumberFormatException e) {
            log.debug("[GPS消费] 坐标或时间格式解析失败，跳过超速检测");
        }
    }

    private void checkStayTooLong(LocationEntity current, String cacheKey, List<LocationEntity> tracePoints) {
        int checkSize = Math.min(STAY_CHECK_WINDOW, tracePoints.size());
        if (checkSize < 2) {
            return;
        }
        List<LocationEntity> recentPoints = tracePoints.subList(tracePoints.size() - checkSize, tracePoints.size());
        LocationEntity first = recentPoints.get(0);
        LocationEntity last = recentPoints.get(checkSize - 1);
        try {
            long stayMinutes = ChronoUnit.MINUTES.between(
                LocalDateTime.parse(first.getCurrentTime(), TIME_FORMATTER),
                LocalDateTime.parse(last.getCurrentTime(), TIME_FORMATTER)
            );
            double lngDiff = Math.abs(Double.parseDouble(last.getLng()) - Double.parseDouble(first.getLng()));
            double latDiff = Math.abs(Double.parseDouble(last.getLat()) - Double.parseDouble(first.getLat()));
            if (stayMinutes > STAY_THRESHOLD_MINUTES
                    && lngDiff < STAY_POSITION_THRESHOLD
                    && latDiff < STAY_POSITION_THRESHOLD) {
                gpsAlertService.alert("STAY_TOO_LONG", current,
                    String.format("长时间停留提醒: 停留时长=%d分钟, 位置=(%s, %s)",
                        stayMinutes, current.getLng(), current.getLat()));
            }
        } catch (Exception e) {
            log.debug("[GPS消费] 时间格式解析失败，跳过停留检测");
        }
    }

    private void checkGeoFence(LocationEntity location, List<LocationEntity> tracePoints) {
        if (tracePoints == null || tracePoints.size() < 3) {
            return;
        }
        double curLng = parseDouble(location.getLng());
        double curLat = parseDouble(location.getLat());
        if (Double.isNaN(curLng) || Double.isNaN(curLat)) {
            return;
        }
        LocationEntity p1 = tracePoints.get(tracePoints.size() - 3);
        LocationEntity p2 = tracePoints.get(tracePoints.size() - 2);
        double p1Lng = parseDouble(p1.getLng());
        double p1Lat = parseDouble(p1.getLat());
        double p2Lng = parseDouble(p2.getLng());
        double p2Lat = parseDouble(p2.getLat());
        if (Double.isNaN(p1Lng) || Double.isNaN(p1Lat) || Double.isNaN(p2Lng) || Double.isNaN(p2Lat)) {
            return;
        }
        double deviateKm = distancePointToSegmentKm(curLng, curLat, p1Lng, p1Lat, p2Lng, p2Lat);
        if (deviateKm > GEO_FENCE_DEVIATE_KM) {
            gpsAlertService.alert("DEVIATE_ROUTE", location,
                String.format("偏离路线提醒: 距历史轨迹%.1fkm, 阈值%.1fkm, 位置=(%s, %s), 运输任务=%s",
                    deviateKm, GEO_FENCE_DEVIATE_KM, location.getLng(), location.getLat(),
                    location.getTransportTaskId()));
        }
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    private double distancePointToSegmentKm(double px, double py, double ax, double ay, double bx, double by) {
        double latScale = 111.0;
        double lngScale = 111.0 * Math.cos(Math.toRadians((py + ay + by) / 3.0));
        double pX = px * lngScale, pY = py * latScale;
        double aX = ax * lngScale, aY = ay * latScale;
        double bX = bx * lngScale, bY = by * latScale;
        double abX = bX - aX, abY = bY - aY;
        double apX = pX - aX, apY = pY - aY;
        double abLenSq = abX * abX + abY * abY;
        if (abLenSq <= 0) {
            return Math.sqrt(apX * apX + apY * apY);
        }
        double t = (apX * abX + apY * abY) / abLenSq;
        t = Math.max(0, Math.min(1, t));
        double footX = aX + t * abX;
        double footY = aY + t * abY;
        double dx = pX - footX, dy = pY - footY;
        return Math.sqrt(dx * dx + dy * dy);
    }

    private long parseTimeDiffSeconds(String currentTimeStr, String prevTimeStr) {
        try {
            LocalDateTime current = LocalDateTime.parse(currentTimeStr, TIME_FORMATTER);
            LocalDateTime prev = LocalDateTime.parse(prevTimeStr, TIME_FORMATTER);
            return ChronoUnit.SECONDS.between(prev, current);
        } catch (Exception e) {
            log.debug("[GPS消费] 时间解析失败: current={}, prev={}", currentTimeStr, prevTimeStr);
            return -1;
        }
    }

    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }

    public String getTraceCacheStats() {
        return String.format("轨迹点总数: %d, 缓存业务对象数: %d", TRACE_COUNT.get(), TRACE_CACHE.size());
    }
}
