package com.xinfei.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xinfei.DTO.OrderDTO;
import com.xinfei.entity.TaskCost;
import com.xinfei.entity.TaskTransport;
import com.xinfei.entity.TransportOrder;
import com.xinfei.entity.TransportOrderTask;
import com.xinfei.feign.OrderFeign;
import com.xinfei.mapper.TaskCostMapper;
import com.xinfei.mapper.TaskTransportMapper;
import com.xinfei.mapper.TransportOrderMapper;
import com.xinfei.mapper.TransportOrderTaskMapper;
import com.xinfei.service.IProfitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 利润查询 Service 实现（P0-6d / FIN-4）
 *
 * <p>数据链（《21》§FIN-4）：work_task_cost(task_transport_id) + TransportOrderTask →
 * TransportOrder.orderId → OrderFeign.findByIds → Order.amount。
 * 一次 feign 批量聚合，避免逐趟 N+1。</p>
 */
@Slf4j
@Service
public class ProfitServiceImpl implements IProfitService {

    private static final int SUMMARY_TRIP_LIMIT = 200;
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    @Autowired
    private TaskCostMapper taskCostMapper;
    @Autowired
    private TaskTransportMapper taskTransportMapper;
    @Autowired
    private TransportOrderMapper transportOrderMapper;
    @Autowired
    private TransportOrderTaskMapper transportOrderTaskMapper;
    @Autowired
    private OrderFeign orderFeign;

    @Override
    public Map<String, Object> taskProfit(Long taskTransportId) {
        // 成本
        List<TaskCost> costItems = taskCostMapper.selectList(new LambdaQueryWrapper<TaskCost>()
                .eq(TaskCost::getTaskTransportId, taskTransportId)
                .orderByDesc(TaskCost::getOccurTime));
        BigDecimal cost = sumCost(costItems);
        // 收入
        List<OrderDTO> orders = tripOrders(taskTransportId);
        BigDecimal income = sumAmount(orders);
        BigDecimal profit = income.subtract(cost);
        Map<String, Object> vo = baseVo(income, cost, profit);
        vo.put("taskId", taskTransportId);
        vo.put("orderCount", orders.size());
        vo.put("costItems", costItems);
        return vo;
    }

    @Override
    public Map<String, Object> orderProfit(Long orderId) {
        // 找该单的运单（V1 取最近一条）→ 其趟
        List<TransportOrder> tos = transportOrderMapper.selectList(new LambdaQueryWrapper<TransportOrder>()
                .eq(TransportOrder::getOrderId, orderId)
                .orderByDesc(TransportOrder::getId));
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal costShare = BigDecimal.ZERO;
        Long taskId = null;
        if (!tos.isEmpty()) {
            TransportOrder to = tos.get(0);
            taskId = to.getId();
            // 趟成本与趟内订单数（V1 equal 均摊）
            List<TaskCost> costItems = taskCostMapper.selectList(new LambdaQueryWrapper<TaskCost>()
                    .eq(TaskCost::getTaskTransportId, taskId));
            BigDecimal tripCost = sumCost(costItems);
            long orderCountInTrip = transportOrderTaskMapper.selectCount(
                    new LambdaQueryWrapper<TransportOrderTask>().eq(TransportOrderTask::getTaskId, taskId));
            costShare = orderCountInTrip > 0
                    ? tripCost.divide(BigDecimal.valueOf(orderCountInTrip), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            List<OrderDTO> orders = orderFeign.findByIds(List.of(String.valueOf(orderId)));
            income = sumAmount(orders);
        }
        BigDecimal profit = income.subtract(costShare);
        Map<String, Object> vo = baseVo(income, costShare, profit);
        vo.put("orderId", orderId);
        vo.put("taskTransportId", taskId);
        vo.put("allocation", "equal(V1 均摊，§6.4a 精细分摊 P1)");
        return vo;
    }

    @Override
    public List<Map<String, Object>> summary() {
        List<Map<String, Object>> trips = tripProfitList(SUMMARY_TRIP_LIMIT);
        Map<String, List<Map<String, Object>>> byMonth = trips.stream()
                .collect(Collectors.groupingBy(t -> String.valueOf(t.get("month")), LinkedHashMap::new, Collectors.toList()));
        return byMonth.entrySet().stream()
                .sorted(Map.Entry.<String, List<Map<String, Object>>>comparingByKey().reversed())
                .map(e -> {
                    List<Map<String, Object>> list = e.getValue();
                    BigDecimal income = list.stream().map(t -> (BigDecimal) t.get("income")).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal cost = list.stream().map(t -> (BigDecimal) t.get("cost")).reduce(BigDecimal.ZERO, BigDecimal::add);
                    Map<String, Object> m = baseVo(income, cost, income.subtract(cost));
                    m.put("month", e.getKey());
                    m.put("tripCount", list.size());
                    return m;
                }).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> ranking() {
        List<Map<String, Object>> trips = tripProfitList(SUMMARY_TRIP_LIMIT);
        trips.sort(Comparator.comparing(t -> (BigDecimal) t.get("profit"), Comparator.reverseOrder()));
        List<Map<String, Object>> top = trips.stream().limit(5).collect(Collectors.toList());
        List<Map<String, Object>> bottom = trips.stream()
                .skip(Math.max(0, trips.size() - 5))
                .sorted(Comparator.comparing(t -> (BigDecimal) t.get("profit")))
                .collect(Collectors.toList());
        Map<String, Object> vo = new HashMap<>();
        vo.put("top", top);
        vo.put("bottom", bottom);
        return vo;
    }

    /**
     * 近 limit 趟的利润列表（一次 feign 批量取订单金额）。
     * 每项：{taskId, month, income, cost, profit, orderCount}
     */
    private List<Map<String, Object>> tripProfitList(int limit) {
        List<TaskTransport> tasks = taskTransportMapper.selectList(new LambdaQueryWrapper<TaskTransport>()
                .orderByDesc(TaskTransport::getId)
                .last("limit " + limit));
        if (tasks.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> taskIds = tasks.stream().map(TaskTransport::getId).collect(Collectors.toList());
        // 趟 → 运单 → 订单
        List<TransportOrderTask> links = transportOrderTaskMapper.selectList(
                new LambdaQueryWrapper<TransportOrderTask>().in(TransportOrderTask::getTaskId, taskIds));
        Map<Long, List<Long>> taskToToIds = links.stream().collect(Collectors.groupingBy(
                TransportOrderTask::getTaskId,
                Collectors.mapping(TransportOrderTask::getTransportOrderId, Collectors.toList())));
        List<Long> toIds = links.stream().map(TransportOrderTask::getTransportOrderId).distinct().collect(Collectors.toList());
        Map<Long, Long> toIdToOrderId = new HashMap<>();
        if (!toIds.isEmpty()) {
            transportOrderMapper.selectList(new LambdaQueryWrapper<TransportOrder>().in(TransportOrder::getId, toIds))
                    .forEach(to -> toIdToOrderId.put(to.getId(), to.getOrderId()));
        }
        // 一次 feign 批量取订单金额
        List<Long> orderIds = toIdToOrderId.values().stream().filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
        Map<String, BigDecimal> orderAmount = new HashMap<>();
        if (!orderIds.isEmpty()) {
            List<String> idStrs = orderIds.stream().map(String::valueOf).collect(Collectors.toList());
            List<OrderDTO> orders = orderFeign.findByIds(idStrs);
            if (orders != null) {
                orders.forEach(o -> orderAmount.put(String.valueOf(o.getId()), o.getAmount() == null ? BigDecimal.ZERO : o.getAmount()));
            }
        }
        // 成本按趟分组
        Map<Long, BigDecimal> costByTask = taskCostMapper.selectList(
                        new LambdaQueryWrapper<TaskCost>().in(TaskCost::getTaskTransportId, taskIds))
                .stream().collect(Collectors.groupingBy(TaskCost::getTaskTransportId,
                        Collectors.reducing(BigDecimal.ZERO, c -> c.getAmount() == null ? BigDecimal.ZERO : c.getAmount(), BigDecimal::add)));
        // 逐趟组装
        List<Map<String, Object>> result = new ArrayList<>();
        for (TaskTransport task : tasks) {
            BigDecimal income = taskToToIds.getOrDefault(task.getId(), List.of()).stream()
                    .map(toIdToOrderId::get).filter(java.util.Objects::nonNull)
                    .map(oid -> orderAmount.getOrDefault(String.valueOf(oid), BigDecimal.ZERO))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal cost = costByTask.getOrDefault(task.getId(), BigDecimal.ZERO);
            Map<String, Object> vo = baseVo(income, cost, income.subtract(cost));
            vo.put("taskId", task.getId());
            vo.put("month", task.getCreateTime() == null ? "" : task.getCreateTime().format(MONTH_FMT));
            result.add(vo);
        }
        return result;
    }

    /** 单趟关联订单（趟→运单→订单，Feign 取详情） */
    private List<OrderDTO> tripOrders(Long taskTransportId) {
        List<TransportOrderTask> links = transportOrderTaskMapper.selectList(
                new LambdaQueryWrapper<TransportOrderTask>().eq(TransportOrderTask::getTaskId, taskTransportId));
        if (links.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> toIds = links.stream().map(TransportOrderTask::getTransportOrderId).collect(Collectors.toList());
        List<Long> orderIds = transportOrderMapper.selectList(
                        new LambdaQueryWrapper<TransportOrder>().in(TransportOrder::getId, toIds))
                .stream().map(TransportOrder::getOrderId).filter(java.util.Objects::nonNull).collect(Collectors.toList());
        if (orderIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<OrderDTO> orders = orderFeign.findByIds(
                orderIds.stream().map(String::valueOf).collect(Collectors.toList()));
        return orders == null ? new ArrayList<>() : orders;
    }

    private BigDecimal sumCost(List<TaskCost> items) {
        return items.stream().map(c -> c.getAmount() == null ? BigDecimal.ZERO : c.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumAmount(List<OrderDTO> orders) {
        return orders.stream().map(o -> o.getAmount() == null ? BigDecimal.ZERO : o.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Map<String, Object> baseVo(BigDecimal income, BigDecimal cost, BigDecimal profit) {
        Map<String, Object> vo = new HashMap<>();
        vo.put("income", income);
        vo.put("cost", cost);
        vo.put("profit", profit);
        if (income.compareTo(BigDecimal.ZERO) > 0) {
            vo.put("marginRate", profit.multiply(BigDecimal.valueOf(100))
                    .divide(income, 2, RoundingMode.HALF_UP) + "%");
        } else {
            vo.put("marginRate", null);
        }
        return vo;
    }
}
