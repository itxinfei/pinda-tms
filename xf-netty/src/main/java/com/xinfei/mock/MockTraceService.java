package com.xinfei.mock;

import com.xinfei.common.context.TenantContextHolder;
import com.xinfei.entity.AlarmRecord;
import com.xinfei.entity.LocationRecord;
import com.xinfei.enums.AlarmTypeEnum;
import com.xinfei.enums.BusinessType;
import com.xinfei.enums.CoordSystem;
import com.xinfei.service.IAlarmRecordService;
import com.xinfei.service.ILocationRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Conditional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * mock 轨迹与告警数据生成（xf-netty 域：iot_truck_location / iot_alarm_record）。
 *
 * <p>识别与清理口径：轨迹 source 恒为 {@code MOCK}（真实来源为 MOBILE/PDA/JT808/
 * NETTY_TCP/HTTP，MOCK 值天然隔离）；告警 alarmContent 前缀 {@code [AUTO-MOCK]}。
 * 车辆轨迹沿武汉→成都干线插值（与 xf-work mock 任务起讫一致），reportTime 为
 * 当天时间戳（管理端"在途监控/运输看板"与客户端轨迹查询立即有数）。</p>
 *
 * <p>跨域只读：车辆牌照经 JdbcTemplate 查 base_truck；运输任务 id 沿
 * AUTO-MOCK 订单→运单→任务关联链查（只读不写）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Conditional(MockGuardCondition.class)
public class MockTraceService {

    /** 轨迹专属数据来源（=清理键，真实来源白名单外天然隔离） */
    public static final String MOCK_SOURCE = "MOCK";
    /** 告警内容前缀（=清理键） */
    public static final String MOCK_ALARM_PREFIX = "[AUTO-MOCK]";

    /** 干线车辆（车队 610 种子车，与 xf-work mock 任务同车池） */
    private static final long[] TRUCK_IDS = {501L, 502L, 503L};
    /** 演示快递员（demo08/demo09，轨迹 businessId=sys_user.id） */
    private static final long[] COURIER_IDS = {9211L, 9212L};
    /** 干线插值端点：武汉 114.30,30.59 → 成都 104.06,30.57（与订单/任务坐标同源） */
    private static final double FROM_LNG = 114.30, FROM_LAT = 30.59, TO_LNG = 104.06, TO_LAT = 30.57;

    private final ILocationRecordService locationRecordService;
    private final IAlarmRecordService alarmRecordService;
    private final DataSource dataSource;

    private JdbcTemplate jdbc() {
        return new JdbcTemplate(dataSource);
    }

    /**
     * 生成当日轨迹点串与未处理告警
     *
     * @param pointsPerTruck 每车轨迹点数（8~50 限幅）
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> generate(int pointsPerTruck) {
        // 系统态租户上下文：docker exec 直调无租户头，多租户插件会回落 tenant_id=0
        // 导致登录租户（1）查不到 mock 数据——必须显式置 1（惯例见 RabbitGpsTraceConsumer）
        TenantContextHolder.setTenantId(1L);
        try {
            return doGenerate(pointsPerTruck);
        } finally {
            TenantContextHolder.clear();
        }
    }

    private Map<String, Object> doGenerate(int pointsPerTruck) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        JdbcTemplate jdbc = jdbc();

        List<Long> taskIds = jdbc.queryForList(
                "SELECT tt.id FROM work_task_transport tt "
                        + "JOIN work_transport_order_task rel ON rel.task_id = tt.id "
                        + "JOIN work_transport_order o ON o.id = rel.transport_order_id "
                        + "JOIN oms_order om ON om.id = o.order_id "
                        + "WHERE om.remark LIKE 'AUTO-MOCK%' LIMIT 5", Long.class);

        int traces = 0;
        LocalDateTime now = LocalDateTime.now();
        // 1) 车辆干线轨迹：今天的连续点串，8:00 起每 20 分钟一点（默认 24 点→8:00~15:40）
        int seq = 0;
        for (Long truckId : TRUCK_IDS) {
            // 车辆可能被清理，查不到牌照不阻断（licensePlate 置空）
            List<String> plates = jdbc.queryForList(
                    "SELECT license_plate FROM base_truck WHERE id = ? AND deleted = 0",
                    String.class, truckId);
            String plate = plates.isEmpty() ? null : plates.get(0);
            Long taskId = taskIds.isEmpty() ? null : taskIds.get(seq % taskIds.size());
            for (int i = 0; i < pointsPerTruck; i++) {
                double ratio = (double) i / Math.max(1, pointsPerTruck - 1);
                locationRecordService.save(buildRecord(rnd,
                        truckId, BusinessType.TRUCK, "测试司机" + (seq + 1), plate, taskId,
                        lerp(FROM_LNG, TO_LNG, ratio), lerp(FROM_LAT, TO_LAT, ratio),
                        now.withHour(8).withMinute(0).withSecond(0).withNano(0).plusMinutes(i * 20L)));
                traces++;
            }
            seq++;
        }

        // 2) 快递员市区轨迹：武汉江岸网点附近抖动，9:00 起每 30 分钟一点
        for (Long courierId : COURIER_IDS) {
            for (int i = 0; i < 6; i++) {
                locationRecordService.save(buildRecord(rnd,
                        courierId, BusinessType.COURIER, "测试快递员" + (courierId == 9211L ? 8 : 9), null, null,
                        114.31 + rnd.nextDouble() * 0.05, 30.60 + rnd.nextDouble() * 0.05,
                        now.withHour(9).withMinute(0).withSecond(0).withNano(0).plusMinutes(i * 30L)));
                traces++;
            }
        }

        // 3) 未处理告警 4 条（告警中心红点）；类型与文案显式配对，不依赖枚举声明顺序
        AlarmTypeEnum[] alarmTypes = {AlarmTypeEnum.SPEED_OVER, AlarmTypeEnum.STAY_TOO_LONG,
                AlarmTypeEnum.DEVIATE_ROUTE, AlarmTypeEnum.VEHICLE_OFFLINE};
        String[] alarmTexts = {"超速告警：车速 112km/h，超过干线限速 100km/h",
                "长时停留：车辆静置超 2 小时（G318 某服务区）",
                "偏离路线：实际位置偏离计划线路 15.6km",
                "车辆离线：GPS 心跳中断超 30 分钟"};
        for (int i = 0; i < alarmTypes.length; i++) {
            AlarmRecord alarm = new AlarmRecord();
            alarm.setAlarmType(alarmTypes[i].getCode());
            alarm.setTruckId(TRUCK_IDS[i % TRUCK_IDS.length]);
            alarm.setDriverId(9111L);
            if (!taskIds.isEmpty()) {
                alarm.setTransportTaskId(taskIds.get(i % taskIds.size()));
            }
            alarm.setLongitude(BigDecimal.valueOf(lerp(FROM_LNG, TO_LNG, 0.3 + i * 0.15)));
            alarm.setLatitude(BigDecimal.valueOf(lerp(FROM_LAT, TO_LAT, 0.3 + i * 0.15)));
            alarm.setAlarmContent(MOCK_ALARM_PREFIX + alarmTexts[i]);
            alarm.setAlarmTime(now.withHour(9 + i).withMinute(15));
            alarm.setStatus(0); // 0未处理
            alarm.setCreateTime(LocalDateTime.now());
            alarmRecordService.save(alarm);
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("tracePoints", traces);
        stats.put("alarms", alarmTypes.length);
        log.info("[mock] netty 生成完成: {}", stats);
        return stats;
    }

    private LocationRecord buildRecord(ThreadLocalRandom rnd, Long businessId, BusinessType type,
                                       String name, String licensePlate, Long taskId,
                                       double lng, double lat, LocalDateTime reportTime) {
        LocationRecord record = new LocationRecord();
        record.setBusinessId(businessId);
        record.setBusinessType(type.getCode());
        record.setName(name);
        record.setPhone("139" + String.format("%08d", rnd.nextInt(100000000)));
        record.setLicensePlate(licensePlate);
        record.setLongitude(BigDecimal.valueOf(lng).setScale(6, java.math.RoundingMode.HALF_UP));
        record.setLatitude(BigDecimal.valueOf(lat).setScale(6, java.math.RoundingMode.HALF_UP));
        record.setCoordSystem(CoordSystem.WGS84.getNumericCode()); // 存库统一 WGS84（D-13 口径）
        record.setSource(MOCK_SOURCE);
        record.setTeam("一车队");
        record.setTransportTaskId(taskId);
        record.setReportTime(reportTime);
        record.setCreateTime(LocalDateTime.now());
        return record;
    }

    private double lerp(double from, double to, double ratio) {
        return from + (to - from) * ratio;
    }

    /** 清理全部 mock 轨迹与告警（物理删，按专属 source / 内容前缀识别，不碰真实数据） */
    public Map<String, Object> cleanup() {
        JdbcTemplate jdbc = jdbc();
        int traces = jdbc.update("DELETE FROM iot_truck_location WHERE source = ?", MOCK_SOURCE);
        int alarms = jdbc.update("DELETE FROM iot_alarm_record WHERE alarm_content LIKE ?", MOCK_ALARM_PREFIX + "%");
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("traces", traces);
        stats.put("alarms", alarms);
        log.info("[mock] netty 清理完成: {}", stats);
        return stats;
    }
}
