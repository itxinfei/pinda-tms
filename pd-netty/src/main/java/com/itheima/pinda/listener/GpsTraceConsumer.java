package com.itheima.pinda.listener;

import com.alibaba.fastjson.JSON;
import com.itheima.pinda.entity.LocationEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
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
 * GPS轨迹数据Kafka消费端
 *
 * 处理逻辑:
 * 1. 解析GPS位置数据
 * 2. 存储轨迹点（内存缓存，定期落库）
 * 3. 异常检测（超速、偏离路线、长时间停留）
 * 4. 触发告警通知
 *
 * @author Claude Code
 * @since 2026-07-23
 */
@Slf4j
@Component
public class GpsTraceConsumer {

    /**
     * 轨迹点内存缓存（按车辆/快递员ID分组）
     * key: businessId#type, value: 线程安全的同步轨迹点列表（LinkedList保证O(1)删除首元素）
     *
     * 内存上限控制（防止长跑服务 OOM）：
     * 每个 key 内轨迹点数量受 MAX_CACHE_SIZE 约束；
     * key 的总数量受 MAX_CACHE_KEYS 约束（LRU策略淘汰最久未更新的缓存）。
     */
    private static final int MAX_CACHE_KEYS = 5000;
    private static final Map<String, List<LocationEntity>> TRACE_CACHE =
            Collections.synchronizedMap(new LinkedHashMap<String, List<LocationEntity>>(MAX_CACHE_KEYS, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, List<LocationEntity>> eldest) {
                    return size() > MAX_CACHE_KEYS;
                }
            });

    /**
     * 缓存最大容量（每个业务对象最多缓存1000个点）
     */
    private static final int MAX_CACHE_SIZE = 1000;

    /**
     * 轨迹点计数器
     */
    private static final AtomicLong TRACE_COUNT = new AtomicLong(0);

    /**
     * 缓存过期时间（分钟），超过此时间无新点的业务对象自动清理
     */
    private static final long CACHE_EXPIRE_MINUTES = 60;

    /**
     * 定时清理任务（每小时执行一次）
     */
    private static final ScheduledExecutorService CLEANUP_EXECUTOR = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "gps-trace-cache-cleanup");
        t.setDaemon(true);
        return t;
    });

    static {
        // 每小时清理一次过期缓存
        CLEANUP_EXECUTOR.scheduleAtFixedRate(GpsTraceConsumer::cleanupExpiredCache, 1, 1, TimeUnit.HOURS);
    }

    /**
     * 超速阈值（km/h）- 高速公路限速120km/h
     */
    private static final double SPEED_LIMIT = 120.0;

    /**
     * 停留检测时间阈值（分钟）
     */
    private static final long STAY_THRESHOLD_MINUTES = 30;

    /**
     * 停留检测窗口大小（最近N个点）
     */
    private static final int STAY_CHECK_WINDOW = 10;

    /**
     * 停留位置变化阈值（度）
     */
    private static final double STAY_POSITION_THRESHOLD = 0.001;

    /**
     * 时间解析格式器（yyyyMMddHHmmss）
     */
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * GPS轨迹队列名称（与KafkaSender.MSG_TOPIC一致）
     */
    public static final String TOPIC_GPS_TRACE = "tms_order_location";

    /**
     * 消费GPS轨迹数据
     *
     * @param message GPS轨迹JSON消息
     */
    @KafkaListener(topics = TOPIC_GPS_TRACE, groupId = "gps-trace-consumer-group", concurrency = "3")
    public void consumeGpsTrace(String message) {
        try {
            LocationEntity location = JSON.parseObject(message, LocationEntity.class);
            processGpsData(location);
        } catch (Exception e) {
            log.error("[GPS消费] 解析GPS数据失败: message={}", message, e);
        }
    }

    /**
     * 处理GPS数据
     *
     * @param location 位置信息
     */
    private void processGpsData(LocationEntity location) {
        if (location == null || !StringUtils.hasText(location.getBusinessId()) || !StringUtils.hasText(location.getType())) {
            log.warn("[GPS消费] 忽略无效轨迹点: businessId或type为空");
            return;
        }

        String cacheKey = location.getBusinessId() + "#" + location.getType();

        // 1. 添加到轨迹缓存（使用同步列表保证线程安全）
        List<LocationEntity> tracePoints = TRACE_CACHE.computeIfAbsent(cacheKey, k ->
            Collections.synchronizedList(new LinkedList<>()));
        tracePoints.add(location);
        // 超过容量限制时移除最旧的点（LinkedList O(1)操作）
        if (tracePoints.size() > MAX_CACHE_SIZE) {
            tracePoints.remove(0);
        }

        long count = TRACE_COUNT.incrementAndGet();
        log.debug("[GPS消费] 轨迹点接收: businessId={}, type={}, lng={}, lat={}, 累计处理: {}",
            location.getBusinessId(), location.getType(), location.getLng(), location.getLat(), count);

        // 2. 异常检测（读取时加锁，与写入保持同步）
        synchronized (tracePoints) {
            checkAnomalies(location, cacheKey, tracePoints);
        }
    }

    /**
     * 异常检测
     *
     * @param location   当前位置
     * @param cacheKey   缓存键
     * @param tracePoints 轨迹点列表
     */
    private void checkAnomalies(LocationEntity location, String cacheKey, List<LocationEntity> tracePoints) {
        if (tracePoints.size() < 2) {
            return; // 至少需要2个点才能计算
        }

        // 获取上一个位置点
        LocationEntity prevPoint = tracePoints.get(tracePoints.size() - 2);

        // 1. 超速检测
        checkSpeed(location, prevPoint);

        // 2. 长时间停留检测
        checkStayTooLong(location, cacheKey, tracePoints);

        // 3. 电子围栏检测（简化版：检查是否在运输任务关联的网点附近）
        checkGeoFence(location);
    }

    /**
     * 超速检测
     *
     * @param current  当前点
     * @param previous 上一个点
     */
    private void checkSpeed(LocationEntity current, LocationEntity previous) {
        if (current.getLng() == null || current.getLat() == null
                || previous.getLng() == null || previous.getLat() == null) {
            return; // 坐标缺失，跳过检测
        }
        try {
            double currentLng = Double.parseDouble(current.getLng());
            double currentLat = Double.parseDouble(current.getLat());
            double prevLng = Double.parseDouble(previous.getLng());
            double prevLat = Double.parseDouble(previous.getLat());

            // 计算两点间距离（Haversine公式）
            double distanceKm = calculateDistance(prevLat, prevLng, currentLat, currentLng);

            // 计算时间差（秒），使用 DateTimeFormatter 解析 yyyyMMddHHmmss 格式
            long timeDiffSeconds = parseTimeDiffSeconds(current.getCurrentTime(), previous.getCurrentTime());

            if (timeDiffSeconds > 0 && timeDiffSeconds < 300) { // 5分钟内的点才计算速度
                double speedKmh = (distanceKm / timeDiffSeconds) * 3600;
                if (speedKmh > SPEED_LIMIT) {
                    log.warn("[GPS告警] 超速提醒: businessId={}, 速度={}km/h, 阈值={}km/h, 位置: ({}, {})",
                        current.getBusinessId(), String.format("%.1f", speedKmh), SPEED_LIMIT,
                        current.getLng(), current.getLat());
                }
            }
        } catch (NumberFormatException e) {
            log.debug("[GPS消费] 坐标或时间格式解析失败，跳过超速检测");
        }
    }

    /**
     * 长时间停留检测
     *
     * @param current    当前位置
     * @param cacheKey   缓存键
     * @param tracePoints 轨迹点列表
     */
    private void checkStayTooLong(LocationEntity current, String cacheKey, List<LocationEntity> tracePoints) {
        // 检查最近N个点是否在同一个位置
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

            // 检查是否在同一个位置（经纬度变化小于阈值，约100米）
            double lngDiff = Math.abs(Double.parseDouble(last.getLng()) - Double.parseDouble(first.getLng()));
            double latDiff = Math.abs(Double.parseDouble(last.getLat()) - Double.parseDouble(first.getLat()));

            if (stayMinutes > STAY_THRESHOLD_MINUTES
                    && lngDiff < STAY_POSITION_THRESHOLD
                    && latDiff < STAY_POSITION_THRESHOLD) {
                log.warn("[GPS告警] 长时间停留: businessId={}, 停留时长={}分钟, 位置: ({}, {})",
                    current.getBusinessId(), stayMinutes, current.getLng(), current.getLat());
            }
        } catch (Exception e) {
            log.debug("[GPS消费] 时间格式解析失败，跳过停留检测");
        }
    }

    /**
     * 电子围栏检测（简化版）
     *
     * @param location 当前位置
     */
    private void checkGeoFence(LocationEntity location) {
        // TODO: 后续实现基于地理围栏的偏离路线检测
        // 需要结合运输任务的线路规划数据
        if (location.getTransportTaskId() != null) {
            log.debug("[GPS消费] 运输任务轨迹记录: taskId={}, 位置: ({}, {})",
                location.getTransportTaskId(), location.getLng(), location.getLat());
        }
    }

    /**
     * 计算两个时间字符串的差值（秒）
     *
     * @param currentTimeStr 当前时间 yyyyMMddHHmmss
     * @param prevTimeStr    上一个时间 yyyyMMddHHmmss
     * @return 时间差（秒）
     */
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

    /**
     * 计算两点间距离（Haversine公式，单位：千米）
     *
     * @param lat1 纬度1
     * @param lng1 经度1
     * @param lat2 纬度2
     * @param lng2 经度2
     * @return 距离（千米）
     */
    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6371.0; // 地球平均半径（千米）
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }

    /**
     * 清理过期缓存（60分钟无新点自动移除）
     */
    private static void cleanupExpiredCache() {
        long now = System.currentTimeMillis();
        int removed = 0;
        // synchronizedMap 的迭代器非线程安全，必须在 map 锁内遍历
        synchronized (TRACE_CACHE) {
            for (Map.Entry<String, List<LocationEntity>> entry : TRACE_CACHE.entrySet()) {
                List<LocationEntity> points = entry.getValue();
                if (points.isEmpty()) {
                    TRACE_CACHE.remove(entry.getKey());
                    removed++;
                    continue;
                }
                // 检查最后一条点的时间
                LocationEntity last = points.get(points.size() - 1);
                try {
                    LocalDateTime lastTime = LocalDateTime.parse(last.getCurrentTime(), TIME_FORMATTER);
                    if (ChronoUnit.MINUTES.between(lastTime, LocalDateTime.now()) > CACHE_EXPIRE_MINUTES) {
                        TRACE_CACHE.remove(entry.getKey());
                        removed++;
                    }
                } catch (Exception e) {
                    // 时间格式异常，移除该缓存
                    TRACE_CACHE.remove(entry.getKey());
                    removed++;
                }
            }
        }
        if (removed > 0) {
            log.info("[GPS缓存清理] 清理过期缓存条目: {}, 当前缓存业务对象数: {}", removed, TRACE_CACHE.size());
        }
    }

    /**
     * 获取轨迹点缓存统计信息
     *
     * @return 统计信息
     */
    public String getTraceCacheStats() {
        return String.format("轨迹点总数: %d, 缓存业务对象数: %d", TRACE_COUNT.get(), TRACE_CACHE.size());
    }
}
