package com.itheima.pinda.state;

import com.itheima.pinda.enums.OrderStatus;
import com.itheima.pinda.enums.transportorder.TransportOrderStatus;
import com.itheima.pinda.enums.transporttask.TransportTaskStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 状态流转校验器
 *
 * 替代Spring Statemachine，实现轻量级状态管理
 * 优势：无额外依赖、性能好、易维护
 *
 * 校验规则：
 * 1. 订单状态流转
 * 2. 运单状态流转
 * 3. 运输任务状态流转
 *
 * @author Claude Code
 * @since 2026-07-01
 */
@Slf4j
@Component
public class StateTransitionValidator {

    /**
     * 订单状态流转图
     * Key: 当前状态
     * Value: 允许的下一个状态集合
     */
    private static final Map<Integer, Set<Integer>> ORDER_STATUS_TRANSITIONS = new HashMap<>();

    /**
     * 运单状态流转图
     */
    private static final Map<Integer, Set<Integer>> TRANSPORT_ORDER_TRANSITIONS = new HashMap<>();

    /**
     * 运输任务状态流转图
     */
    private static final Map<Integer, Set<Integer>> TRANSPORT_TASK_TRANSITIONS = new HashMap<>();

    static {
        // 初始化订单状态流转规则
        initOrderStatusTransitions();

        // 初始化运单状态流转规则
        initTransportOrderTransitions();

        // 初始化运输任务状态流转规则
        initTransportTaskTransitions();
    }

    /**
     * 初始化订单状态流转规则
     *
     * 待取件(23000) → 已取件(23001)
     * 已取件(23001) → 网点入库(23003)
     * 网点入库(23003) → 待装车(23004)
     * 待装车(23004) → 运输中(23005)
     * 运输中(23005) → 网点出库(23006)
     * 网点出库(23006) → 待派送(23007)
     * 待派送(23007) → 派送中(23008)
     * 派送中(23008) → 已签收(23009) / 拒收(23010)
     * 任何状态 → 已取消(230011)（取消操作）
     */
    private static void initOrderStatusTransitions() {
        // 待取件 → 已取件 / 已取消
        ORDER_STATUS_TRANSITIONS.put(OrderStatus.PENDING.getCode(), new HashSet<>(Arrays.asList(
            OrderStatus.PICKED_UP.getCode(),
            OrderStatus.CANCELLED.getCode()
        )));

        // 已取件 → 网点入库 / 已取消
        ORDER_STATUS_TRANSITIONS.put(OrderStatus.PICKED_UP.getCode(), new HashSet<>(Arrays.asList(
            OrderStatus.OUTLETS_WAREHOUSE.getCode(),
            OrderStatus.CANCELLED.getCode()
        )));

        // 网点入库 → 待装车 / 已取消
        ORDER_STATUS_TRANSITIONS.put(OrderStatus.OUTLETS_WAREHOUSE.getCode(), new HashSet<>(Arrays.asList(
            OrderStatus.FOR_LOADING.getCode(),
            OrderStatus.CANCELLED.getCode()
        )));

        // 待装车 → 运输中 / 已取消
        ORDER_STATUS_TRANSITIONS.put(OrderStatus.FOR_LOADING.getCode(), new HashSet<>(Arrays.asList(
            OrderStatus.IN_TRANSIT.getCode(),
            OrderStatus.CANCELLED.getCode()
        )));

        // 运输中 → 网点出库 / 已取消
        ORDER_STATUS_TRANSITIONS.put(OrderStatus.IN_TRANSIT.getCode(), new HashSet<>(Arrays.asList(
            OrderStatus.OUTLETS_EX_WAREHOUSE.getCode(),
            OrderStatus.CANCELLED.getCode()
        )));

        // 网点出库 → 待派送 / 已取消
        ORDER_STATUS_TRANSITIONS.put(OrderStatus.OUTLETS_EX_WAREHOUSE.getCode(), new HashSet<>(Arrays.asList(
            OrderStatus.TO_BE_DISPATCHED.getCode(),
            OrderStatus.CANCELLED.getCode()
        )));

        // 待派送 → 派送中 / 已取消
        ORDER_STATUS_TRANSITIONS.put(OrderStatus.TO_BE_DISPATCHED.getCode(), new HashSet<>(Arrays.asList(
            OrderStatus.DISPATCHING.getCode(),
            OrderStatus.CANCELLED.getCode()
        )));

        // 派送中 → 已签收 / 拒收 / 已取消
        ORDER_STATUS_TRANSITIONS.put(OrderStatus.DISPATCHING.getCode(), new HashSet<>(Arrays.asList(
            OrderStatus.RECEIVED.getCode(),
            OrderStatus.REJECTION.getCode(),
            OrderStatus.CANCELLED.getCode()
        )));

        // 已签收、拒收、已取消 → 终态，不能流转
        ORDER_STATUS_TRANSITIONS.put(OrderStatus.RECEIVED.getCode(), Collections.emptySet());
        ORDER_STATUS_TRANSITIONS.put(OrderStatus.REJECTION.getCode(), Collections.emptySet());
        ORDER_STATUS_TRANSITIONS.put(OrderStatus.CANCELLED.getCode(), Collections.emptySet());
    }

    /**
     * 初始化运单状态流转规则
     *
     * 新建(1) → 待调度(1)
     * 待调度(1) → 已调度(3)
     * 已调度(3) → 已装车(2)
     * 已装车(2) → 到达(3)
     * 到达(3) → 到达终端网点(4)
     * 到达终端网点(4) → 已签收(5) / 拒收(6)
     */
    private static void initTransportOrderTransitions() {
        // 新建(1) → 待调度(1)
        TRANSPORT_ORDER_TRANSITIONS.put(TransportOrderStatus.CREATED.getCode(), new HashSet<>(Arrays.asList(
            TransportOrderStatus.TO_BE_SCHEDULED.getCode()
        )));

        // 待调度(1) → 已调度(3)
        TRANSPORT_ORDER_TRANSITIONS.put(TransportOrderStatus.TO_BE_SCHEDULED.getCode(), new HashSet<>(Arrays.asList(
            TransportOrderStatus.SCHEDULED.getCode()
        )));

        // 已调度(3) → 已装车(2)
        TRANSPORT_ORDER_TRANSITIONS.put(TransportOrderStatus.SCHEDULED.getCode(), new HashSet<>(Arrays.asList(
            TransportOrderStatus.LOADED.getCode()
        )));

        // 已装车(2) → 到达(3)
        TRANSPORT_ORDER_TRANSITIONS.put(TransportOrderStatus.LOADED.getCode(), new HashSet<>(Arrays.asList(
            TransportOrderStatus.ARRIVED.getCode()
        )));

        // 到达(3) → 到达终端网点(4)
        TRANSPORT_ORDER_TRANSITIONS.put(TransportOrderStatus.ARRIVED.getCode(), new HashSet<>(Arrays.asList(
            TransportOrderStatus.ARRIVED_END.getCode()
        )));

        // 到达终端网点(4) → 已签收(5) / 拒收(6)
        TRANSPORT_ORDER_TRANSITIONS.put(TransportOrderStatus.ARRIVED_END.getCode(), new HashSet<>(Arrays.asList(
            TransportOrderStatus.RECEIVED.getCode(),
            TransportOrderStatus.REJECTED.getCode()
        )));

        // 已签收、拒收 → 终态
        TRANSPORT_ORDER_TRANSITIONS.put(TransportOrderStatus.RECEIVED.getCode(), Collections.emptySet());
        TRANSPORT_ORDER_TRANSITIONS.put(TransportOrderStatus.REJECTED.getCode(), Collections.emptySet());
    }

    /**
     * 初始化运输任务状态流转规则
     *
     * 待执行(1) → 进行中(2)
     * 进行中(2) → 待确认(3)
     * 待确认(3) → 已完成(4) / 已取消(5)
     */
    private static void initTransportTaskTransitions() {
        // 待执行 → 进行中
        TRANSPORT_TASK_TRANSITIONS.put(TransportTaskStatus.PENDING.getCode(), new HashSet<>(Collections.singletonList(
            TransportTaskStatus.IN_PROGRESS.getCode()
        )));

        // 进行中 → 待确认
        TRANSPORT_TASK_TRANSITIONS.put(TransportTaskStatus.IN_PROGRESS.getCode(), new HashSet<>(Collections.singletonList(
            TransportTaskStatus.WAITING_CONFIRM.getCode()
        )));

        // 待确认 → 已完成 / 已取消
        TRANSPORT_TASK_TRANSITIONS.put(TransportTaskStatus.WAITING_CONFIRM.getCode(), new HashSet<>(Arrays.asList(
            TransportTaskStatus.COMPLETED.getCode(),
            TransportTaskStatus.CANCELLED.getCode()
        )));

        // 已完成、已取消 → 终态
        TRANSPORT_TASK_TRANSITIONS.put(TransportTaskStatus.COMPLETED.getCode(), Collections.emptySet());
        TRANSPORT_TASK_TRANSITIONS.put(TransportTaskStatus.CANCELLED.getCode(), Collections.emptySet());
    }

    /**
     * 校验订单状态流转是否合法
     *
     * @param currentStatus 当前状态
     * @param targetStatus 目标状态
     * @return 是否允许流转
     */
    public boolean validateOrderStatusTransition(Integer currentStatus, Integer targetStatus) {
        if (currentStatus == null || targetStatus == null) {
            log.warn("订单状态校验失败：状态值不能为空");
            return false;
        }

        Set<Integer> allowedTransitions = ORDER_STATUS_TRANSITIONS.get(currentStatus);
        if (allowedTransitions == null) {
            log.warn("订单状态校验失败：当前状态[{}]不存在或为终态", currentStatus);
            return false;
        }

        boolean allowed = allowedTransitions.contains(targetStatus);
        if (!allowed) {
            log.warn("订单状态流转非法：当前状态[{}]不能流转到目标状态[{}]",
                OrderStatus.lookup(currentStatus),
                OrderStatus.lookup(targetStatus));
        }

        return allowed;
    }

    /**
     * 校验运单状态流转是否合法
     *
     * @param currentStatus 当前状态
     * @param targetStatus 目标状态
     * @return 是否允许流转
     */
    public boolean validateTransportOrderTransition(Integer currentStatus, Integer targetStatus) {
        if (currentStatus == null || targetStatus == null) {
            log.warn("运单状态校验失败：状态值不能为空");
            return false;
        }

        Set<Integer> allowedTransitions = TRANSPORT_ORDER_TRANSITIONS.get(currentStatus);
        if (allowedTransitions == null) {
            log.warn("运单状态校验失败：当前状态[{}]不存在或为终态", currentStatus);
            return false;
        }

        boolean allowed = allowedTransitions.contains(targetStatus);
        if (!allowed) {
            log.warn("运单状态流转非法：当前状态[{}]不能流转到目标状态[{}]",
                TransportOrderStatus.lookup(currentStatus),
                TransportOrderStatus.lookup(targetStatus));
        }

        return allowed;
    }

    /**
     * 校验运输任务状态流转是否合法
     *
     * @param currentStatus 当前状态
     * @param targetStatus 目标状态
     * @return 是否允许流转
     */
    public boolean validateTransportTaskTransition(Integer currentStatus, Integer targetStatus) {
        if (currentStatus == null || targetStatus == null) {
            log.warn("运输任务状态校验失败：状态值不能为空");
            return false;
        }

        Set<Integer> allowedTransitions = TRANSPORT_TASK_TRANSITIONS.get(currentStatus);
        if (allowedTransitions == null) {
            log.warn("运输任务状态校验失败：当前状态[{}]不存在或为终态", currentStatus);
            return false;
        }

        boolean allowed = allowedTransitions.contains(targetStatus);
        if (!allowed) {
            log.warn("运输任务状态流转非法：当前状态[{}]不能流转到目标状态[{}]",
                TransportTaskStatus.lookup(currentStatus),
                TransportTaskStatus.lookup(targetStatus));
        }

        return allowed;
    }

    /**
     * 获取订单允许的下一个状态
     *
     * @param currentStatus 当前状态
     * @return 允许的下一个状态集合
     */
    public Set<Integer> getAllowedNextOrderStatuses(Integer currentStatus) {
        return ORDER_STATUS_TRANSITIONS.getOrDefault(currentStatus, Collections.emptySet());
    }

    /**
     * 获取运单允许的下一个状态
     *
     * @param currentStatus 当前状态
     * @return 允许的下一个状态集合
     */
    public Set<Integer> getAllowedNextTransportOrderStatuses(Integer currentStatus) {
        return TRANSPORT_ORDER_TRANSITIONS.getOrDefault(currentStatus, Collections.emptySet());
    }

    /**
     * 获取运输任务允许的下一个状态
     *
     * @param currentStatus 当前状态
     * @return 允许的下一个状态集合
     */
    public Set<Integer> getAllowedNextTransportTaskStatuses(Integer currentStatus) {
        return TRANSPORT_TASK_TRANSITIONS.getOrDefault(currentStatus, Collections.emptySet());
    }
}
