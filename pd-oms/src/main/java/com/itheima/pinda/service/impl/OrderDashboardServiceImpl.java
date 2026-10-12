package com.itheima.pinda.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.itheima.pinda.DTO.dashboard.OrderOverviewDTO;
import com.itheima.pinda.DTO.dashboard.OrderTrendDTO;
import com.itheima.pinda.DTO.dashboard.PendingOrderDTO;
import com.itheima.pinda.entity.Order;
import com.itheima.pinda.enums.OrderStatus;
import com.itheima.pinda.mapper.OrderMapper;
import com.itheima.pinda.service.IOrderDashboardService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理端工作台-订单统计只读服务实现。
 *
 * <p>所有查询均走 MyBatis-Plus 内置方法（count/page/selectMaps），逻辑删除 deleted=0 自动追加；
 * 多租户 tenant_id 由 TenantLineInnerInterceptor 解析 SQL 自动追加，与订单列表页同口径，
 * 本类不手写租户列。</p>
 */
@Service
public class OrderDashboardServiceImpl extends ServiceImpl<OrderMapper, Order> implements IOrderDashboardService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 待处理桶：待取件/已取件/网点自寄/网点入库/待装车 */
    private static final Set<OrderStatus> PENDING_BUCKET = EnumSet.of(
            OrderStatus.PENDING,
            OrderStatus.PICKED_UP,
            OrderStatus.OUTLETS_SINCE_SENT,
            OrderStatus.OUTLETS_WAREHOUSE,
            OrderStatus.FOR_LOADING);

    /** 运输中桶：运输中/网点出库/待派送/派送中 */
    private static final Set<OrderStatus> IN_TRANSIT_BUCKET = EnumSet.of(
            OrderStatus.IN_TRANSIT,
            OrderStatus.OUTLETS_EX_WAREHOUSE,
            OrderStatus.TO_BE_DISPATCHED,
            OrderStatus.DISPATCHING);

    /** 状态码 → 中文（唯一真源《07-数据字典与枚举口径》OrderStatus；集中于此，不外泄到前端） */
    private static final Map<OrderStatus, String> STATUS_TEXT = new EnumMap<>(OrderStatus.class);

    static {
        STATUS_TEXT.put(OrderStatus.PENDING, "待取件");
        STATUS_TEXT.put(OrderStatus.PICKED_UP, "已取件");
        STATUS_TEXT.put(OrderStatus.OUTLETS_SINCE_SENT, "网点自寄");
        STATUS_TEXT.put(OrderStatus.OUTLETS_WAREHOUSE, "网点入库");
        STATUS_TEXT.put(OrderStatus.FOR_LOADING, "待装车");
        STATUS_TEXT.put(OrderStatus.IN_TRANSIT, "运输中");
        STATUS_TEXT.put(OrderStatus.OUTLETS_EX_WAREHOUSE, "网点出库");
        STATUS_TEXT.put(OrderStatus.TO_BE_DISPATCHED, "待派送");
        STATUS_TEXT.put(OrderStatus.DISPATCHING, "派送中");
        STATUS_TEXT.put(OrderStatus.RECEIVED, "已签收");
        STATUS_TEXT.put(OrderStatus.REJECTION, "拒收");
        STATUS_TEXT.put(OrderStatus.CANCELLED, "已取消");
    }

    @Override
    public OrderOverviewDTO overview() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        long todayCount = this.count(new LambdaQueryWrapper<Order>()
                .ge(Order::getCreateTime, todayStart));
        long pendingCount = this.count(new LambdaQueryWrapper<Order>()
                .in(Order::getStatus, codes(PENDING_BUCKET)));
        long inTransitCount = this.count(new LambdaQueryWrapper<Order>()
                .in(Order::getStatus, codes(IN_TRANSIT_BUCKET)));
        long completedCount = this.count(new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, OrderStatus.RECEIVED.getCode()));
        // 今日签收：oms_order 无签收完成时间列，按 update_time 当日 + 已签收近似（与 trend 签收曲线同口径）
        long todayCompletedCount = this.count(new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, OrderStatus.RECEIVED.getCode())
                .ge(Order::getUpdateTime, todayStart));

        OrderOverviewDTO dto = new OrderOverviewDTO();
        dto.setTodayCount(todayCount);
        dto.setPendingCount(pendingCount);
        dto.setInTransitCount(inTransitCount);
        dto.setCompletedCount(completedCount);
        dto.setTodayCompletedCount(todayCompletedCount);
        return dto;
    }

    @Override
    public List<OrderTrendDTO> trend(Integer days) {
        int n = clamp(days, 7, 1, 30);
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays((long) n - 1);
        LocalDateTime since = start.atStartOfDay();

        // 下单量：按 create_time 归属日期
        Map<String, Long> createdMap = dailyCount(
                "DATE(create_time)", "create_time", since, null);
        // 签收量：oms_order 无签收完成时间列，按 update_time + 已签收近似（DTO 已注明口径缺陷）
        Map<String, Long> completedMap = dailyCount(
                "DATE(update_time)", "update_time", since, OrderStatus.RECEIVED.getCode());

        List<OrderTrendDTO> result = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            LocalDate d = start.plusDays(i);
            String key = d.toString();
            result.add(new OrderTrendDTO(
                    key,
                    createdMap.getOrDefault(key, 0L),
                    completedMap.getOrDefault(key, 0L)));
        }
        return result;
    }

    @Override
    public List<PendingOrderDTO> pendingOrders(Integer limit) {
        int size = clamp(limit, 5, 1, 20);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .select(Order::getId, Order::getOrderNo, Order::getSenderName,
                        Order::getStatus, Order::getCreateTime)
                .in(Order::getStatus, codes(PENDING_BUCKET))
                .orderByDesc(Order::getId);
        IPage<Order> page = this.page(new Page<>(1, size), wrapper);

        return page.getRecords().stream().map(o -> {
            PendingOrderDTO dto = new PendingOrderDTO();
            dto.setId(String.valueOf(o.getId()));
            // order_no 历史数据可能为空，回退用雪花 id 展示，避免空白
            dto.setOrderNo(o.getOrderNo() != null && !o.getOrderNo().isEmpty()
                    ? o.getOrderNo() : String.valueOf(o.getId()));
            dto.setSenderName(o.getSenderName());
            dto.setStatus(o.getStatus());
            dto.setStatusText(statusText(o.getStatus()));
            dto.setCreateTime(o.getCreateTime() == null ? null : TIME_FMT.format(o.getCreateTime()));
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * 按日聚合计数。
     *
     * @param dateExpr 日期分组表达式（DATE(列)）
     * @param timeCol  时间过滤列
     * @param since    起始时间（含）
     * @param status   状态过滤，null 表示不限状态
     * @return 日期(yyyy-MM-dd) → 计数
     */
    private Map<String, Long> dailyCount(String dateExpr, String timeCol,
                                         LocalDateTime since, Integer status) {
        QueryWrapper<Order> wrapper = new QueryWrapper<Order>()
                .select(dateExpr + " AS date", "COUNT(*) AS cnt")
                .ge(timeCol, since)
                .eq(status != null, "status", status)
                .groupBy(dateExpr);
        List<Map<String, Object>> rows = this.baseMapper.selectMaps(wrapper);

        Map<String, Long> map = new HashMap<>(rows.size() * 2);
        for (Map<String, Object> row : rows) {
            Object dateVal = row.get("date");
            Object cntVal = row.get("cnt");
            if (dateVal == null) {
                continue;
            }
            // DATE() 可能返回 LocalDate / java.sql.Date / String，统一截前 10 位为 yyyy-MM-dd
            String key = String.valueOf(dateVal);
            if (key.length() >= 10) {
                key = key.substring(0, 10);
            }
            long cnt = cntVal instanceof Number ? ((Number) cntVal).longValue() : 0L;
            map.put(key, cnt);
        }
        return map;
    }

    private List<Integer> codes(Set<OrderStatus> statuses) {
        return statuses.stream().map(OrderStatus::getCode).collect(Collectors.toList());
    }

    private String statusText(Integer code) {
        OrderStatus status = OrderStatus.lookup(code);
        return status == null ? "" : STATUS_TEXT.getOrDefault(status, "");
    }

    private int clamp(Integer value, int fallback, int min, int max) {
        if (value == null) {
            return fallback;
        }
        return Math.max(min, Math.min(max, value));
    }
}
