package com.itheima.pinda.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.itheima.pinda.DTO.dashboard.NameValueDTO;
import com.itheima.pinda.DTO.dashboard.TaskDashboardSummaryDTO;
import com.itheima.pinda.DTO.dashboard.TripsVolumeDTO;
import com.itheima.pinda.entity.TaskTransport;
import com.itheima.pinda.entity.TransportOrder;
import com.itheima.pinda.enums.transporttask.TransportTaskStatus;
import com.itheima.pinda.mapper.TaskTransportMapper;
import com.itheima.pinda.mapper.TransportOrderMapper;
import com.itheima.pinda.service.ITaskDashboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 运输域统计服务实现。
 *
 * <p>全部走 MyBatis-Plus QueryWrapper.selectMaps 聚合，不写 XML；
 * 多租户 tenant_id 与逻辑删除 deleted=0 由 MP 拦截器自动追加，无需手写。</p>
 */
@Slf4j
@Service
public class TaskDashboardServiceImpl
        extends ServiceImpl<TaskTransportMapper, TaskTransport> implements ITaskDashboardService {

    @Autowired
    private TransportOrderMapper transportOrderMapper;

    /**
     * 车次运量柱状最多返回条数
     */
    private static final int MAX_TRIPS_ITEMS = 10;

    /**
     * 车次运量统计天数上下限
     */
    private static final int MIN_DAYS = 1;
    private static final int MAX_DAYS = 30;
    private static final int DEFAULT_DAYS = 7;

    @Override
    public TaskDashboardSummaryDTO summary() {
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);

        TaskDashboardSummaryDTO dto = new TaskDashboardSummaryDTO();
        dto.setTodayTransportOrderCount(countTodayTransportOrders(todayStart));
        dto.setTodayTaskCount(this.count(new QueryWrapper<TaskTransport>()
                .ge("create_time", todayStart)));
        dto.setInTransitTruckCount(countDistinctTrucks(
                Collections.singletonList(TransportTaskStatus.PROCESSING.getCode())));
        dto.setPendingTruckCount(countDistinctTrucks(
                Arrays.asList(TransportTaskStatus.PENDING.getCode(), TransportTaskStatus.CONFIRM.getCode())));
        dto.setTaskStatusCounts(countGroupByStatus());
        // 准时率：口径与司机绩效「准点率」一致（docs/需求文档/20），全局聚合版。
        // 已送达=actual_arrival_time 非空；准时=actual_arrival_time <= plan_arrival_time。
        dto.setOnTimeRate(computeOnTimeRate());
        return dto;
    }

    /**
     * 准时率（百分比，0~100，整数）：已送达任务中实际到达不晚于计划到达的占比。
     * 无已送达任务（actual_arrival_time 全空）返回 null，前端显示「--」。
     * 口径对齐 docs/需求文档/20 司机绩效「准点率」（actualArrivalTime <= planArrivalTime 趟次 ÷ 总趟次）。
     */
    private Integer computeOnTimeRate() {
        QueryWrapper<TaskTransport> wrapper = new QueryWrapper<>();
        wrapper.select(
                "COUNT(*) AS delivered",
                "SUM(CASE WHEN actual_arrival_time <= plan_arrival_time THEN 1 ELSE 0 END) AS onTime")
                .isNotNull("actual_arrival_time");
        List<Map<String, Object>> rows = baseMapper.selectMaps(wrapper);
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        long delivered = toLong(rows.get(0).get("delivered"));
        if (delivered == 0) {
            return null;
        }
        long onTime = toLong(rows.get(0).get("onTime"));
        return (int) Math.round((double) onTime / delivered * 100);
    }

    @Override
    public List<TripsVolumeDTO> tripsVolume(Integer days) {
        int d = days == null ? DEFAULT_DAYS : Math.max(MIN_DAYS, Math.min(MAX_DAYS, days));
        LocalDateTime since = LocalDateTime.of(LocalDate.now().minusDays((long) d - 1), LocalTime.MIN);

        QueryWrapper<TaskTransport> wrapper = new QueryWrapper<>();
        wrapper.select("trips_id AS tripsId", "COUNT(*) AS taskCount")
                .ge("create_time", since)
                .isNotNull("trips_id")
                .groupBy("trips_id")
                .orderByDesc("taskCount")
                // 常量拼接，无外部输入，不存在注入
                .last("LIMIT " + MAX_TRIPS_ITEMS);

        List<Map<String, Object>> rows = baseMapper.selectMaps(wrapper);
        List<TripsVolumeDTO> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Object tripsId = row.get("tripsId");
            if (tripsId == null) {
                // 兜底：部分数据库驱动返回原始列名
                tripsId = row.get("trips_id");
            }
            if (tripsId == null) {
                continue;
            }
            result.add(new TripsVolumeDTO(String.valueOf(tripsId), toLong(row.get("taskCount"))));
        }
        return result;
    }

    /**
     * 今日新增运单数：按 create_time 当日对 order_id 去重计数。
     */
    private Long countTodayTransportOrders(LocalDateTime todayStart) {
        QueryWrapper<TransportOrder> wrapper = new QueryWrapper<>();
        wrapper.select("COUNT(DISTINCT order_id) AS cnt")
                .ge("create_time", todayStart)
                .isNotNull("order_id");
        return firstColumnAsLong(transportOrderMapper.selectMaps(wrapper));
    }

    /**
     * 按状态集合统计去重车辆数（OPS-3：车辆作业状态由运输任务推导）。
     */
    private Long countDistinctTrucks(List<Integer> statuses) {
        QueryWrapper<TaskTransport> wrapper = new QueryWrapper<>();
        wrapper.select("COUNT(DISTINCT truck_id) AS cnt")
                .in("status", statuses)
                .isNotNull("truck_id");
        return firstColumnAsLong(baseMapper.selectMaps(wrapper));
    }

    /**
     * 运输任务按状态分组计数，按枚举固定顺序补零（前端环形图不缺扇区）。
     */
    private List<NameValueDTO> countGroupByStatus() {
        QueryWrapper<TaskTransport> wrapper = new QueryWrapper<>();
        wrapper.select("status AS status", "COUNT(*) AS cnt")
                .groupBy("status");
        List<Map<String, Object>> rows = baseMapper.selectMaps(wrapper);

        Map<Integer, Long> counted = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object status = row.get("status");
            if (status instanceof Number) {
                counted.put(((Number) status).intValue(), toLong(row.get("cnt")));
            }
        }

        List<NameValueDTO> result = new ArrayList<>();
        for (TransportTaskStatus status : TransportTaskStatus.values()) {
            result.add(new NameValueDTO(status.getValue(), counted.getOrDefault(status.getCode(), 0L)));
        }
        return result;
    }

    /**
     * 取单行聚合查询的 cnt 列；无行返回 0。
     */
    private Long firstColumnAsLong(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return 0L;
        }
        return toLong(rows.get(0).get("cnt"));
    }

    private Long toLong(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }
}
