package com.itheima.pinda.state;

import com.itheima.pinda.enums.OrderStatus;
import com.itheima.pinda.enums.transportorder.TransportOrderSchedulingStatus;
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
     * 运单-状态 流转图（维度一：status 字段）
     * 注意：运单同时存在 status 与 schedulingStatus 两个独立维度，
     * 二者 code 取值重叠（如 CREATED=1 与 TO_BE_SCHEDULED=1），
     * 因此必须拆分为两张独立流转图，禁止共用同一 Map 的 code 作为 key。
     */
    private static final Map<Integer, Set<Integer>> TRANSPORT_ORDER_STATUS_TRANSITIONS = new HashMap<>();

    /**
     * 运单-调度状态 流转图（维度二：schedulingStatus 字段）
     */
    private static final Map<Integer, Set<Integer>> TRANSPORT_ORDER_SCHEDULING_TRANSITIONS = new HashMap<>();

    /**
     * 运输任务状态流转图
     */
    private static final Map<Integer, Set<Integer>> TRANSPORT_TASK_TRANSITIONS = new HashMap<>();

    static {
        // 初始化订单状态流转规则
        initOrderStatusTransitions();

        // 初始化运单状态流转规则（状态维度与调度状态维度分离，避免 code 冲突）
        initTransportOrderStatusTransitions();
        initTransportOrderSchedulingStatusTransitions();

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
     * 任何状态 → 已取消(23011)（取消操作）
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
     * 初始化运单-状态(status)维度流转规则
     *
     * 运单 status 同时承载两种业务子流程（共用同一字段，无法区分）：
     *   - 司机干线/支线：新建(1) → 已装车(2) → 到达(3)/到达终端网点(4) → 已签收(5)/拒收(6)
     *   - 快递员末端派送：新建(1) → 已签收(5)/拒收(6)
     * 因此流转图取两条实际代码路径的并集，仅拦截明显非法的回退/跳跃。
     *
     * 新建(1) → 已装车(2) / 已签收(5) / 拒收(6)
     * 已装车(2) → 到达(3) / 到达终端网点(4)
     * 到达(3) → 到达终端网点(4)
     * 到达终端网点(4) → 已签收(5) / 拒收(6)
     */
    private static void initTransportOrderStatusTransitions() {
        // 新建 → 已装车 / 已签收 / 拒收（司机装车 或 快递员末端直签）
        TRANSPORT_ORDER_STATUS_TRANSITIONS.put(TransportOrderStatus.CREATED.getCode(), new HashSet<>(Arrays.asList(
            TransportOrderStatus.LOADED.getCode(),
            TransportOrderStatus.RECEIVED.getCode(),
            TransportOrderStatus.REJECTED.getCode()
        )));

        // 已装车 → 到达 / 到达终端网点（是否终点网点决定跳到 ARRIVED 还是 ARRIVED_END）
        TRANSPORT_ORDER_STATUS_TRANSITIONS.put(TransportOrderStatus.LOADED.getCode(), new HashSet<>(Arrays.asList(
            TransportOrderStatus.ARRIVED.getCode(),
            TransportOrderStatus.ARRIVED_END.getCode()
        )));

        // 到达 → 到达终端网点
        TRANSPORT_ORDER_STATUS_TRANSITIONS.put(TransportOrderStatus.ARRIVED.getCode(), new HashSet<>(Collections.singletonList(
            TransportOrderStatus.ARRIVED_END.getCode()
        )));

        // 到达终端网点 → 已签收 / 拒收
        TRANSPORT_ORDER_STATUS_TRANSITIONS.put(TransportOrderStatus.ARRIVED_END.getCode(), new HashSet<>(Arrays.asList(
            TransportOrderStatus.RECEIVED.getCode(),
            TransportOrderStatus.REJECTED.getCode()
        )));

        // 已签收、拒收 → 终态
        TRANSPORT_ORDER_STATUS_TRANSITIONS.put(TransportOrderStatus.RECEIVED.getCode(), Collections.emptySet());
        TRANSPORT_ORDER_STATUS_TRANSITIONS.put(TransportOrderStatus.REJECTED.getCode(), Collections.emptySet());
    }

    /**
     * 初始化运单-调度状态(schedulingStatus)维度流转规则
     *
     * 待调度(1) → 已调度(3)
     * 未匹配到线路(2) → 已调度(3)（允许重新调度）
     * 已调度(3) → 终态
     */
    private static void initTransportOrderSchedulingStatusTransitions() {
        // 待调度 → 已调度
        TRANSPORT_ORDER_SCHEDULING_TRANSITIONS.put(TransportOrderSchedulingStatus.TO_BE_SCHEDULED.getCode(), new HashSet<>(Collections.singletonList(
            TransportOrderSchedulingStatus.SCHEDULED.getCode()
        )));

        // 未匹配到线路 → 已调度（允许人工/自动重新调度）
        TRANSPORT_ORDER_SCHEDULING_TRANSITIONS.put(TransportOrderSchedulingStatus.NO_MATCH_TRANSPORTLINE.getCode(), new HashSet<>(Collections.singletonList(
            TransportOrderSchedulingStatus.SCHEDULED.getCode()
        )));

        // 已调度 → 终态
        TRANSPORT_ORDER_SCHEDULING_TRANSITIONS.put(TransportOrderSchedulingStatus.SCHEDULED.getCode(), Collections.emptySet());
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
            TransportTaskStatus.PROCESSING.getCode()
        )));

        // 进行中 → 待确认
        TRANSPORT_TASK_TRANSITIONS.put(TransportTaskStatus.PROCESSING.getCode(), new HashSet<>(Collections.singletonList(
            TransportTaskStatus.CONFIRM.getCode()
        )));

        // 待确认 → 已完成 / 已取消
        TRANSPORT_TASK_TRANSITIONS.put(TransportTaskStatus.CONFIRM.getCode(), new HashSet<>(Arrays.asList(
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
     * 校验运单-状态(status)维度流转是否合法
     *
     * @param currentStatus 当前状态（TransportOrderStatus.code）
     * @param targetStatus 目标状态（TransportOrderStatus.code）
     * @return 是否允许流转
     */
    public boolean validateTransportOrderStatusTransition(Integer currentStatus, Integer targetStatus) {
        if (currentStatus == null || targetStatus == null) {
            log.warn("运单状态校验失败：状态值不能为空");
            return false;
        }

        Set<Integer> allowedTransitions = TRANSPORT_ORDER_STATUS_TRANSITIONS.get(currentStatus);
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
     * 校验运单-调度状态(schedulingStatus)维度流转是否合法
     *
     * @param currentSchedulingStatus 当前调度状态（TransportOrderSchedulingStatus.code）
     * @param targetSchedulingStatus 目标调度状态（TransportOrderSchedulingStatus.code）
     * @return 是否允许流转
     */
    public boolean validateTransportOrderSchedulingTransition(Integer currentSchedulingStatus, Integer targetSchedulingStatus) {
        if (currentSchedulingStatus == null || targetSchedulingStatus == null) {
            log.warn("运单调度状态校验失败：状态值不能为空");
            return false;
        }

        Set<Integer> allowedTransitions = TRANSPORT_ORDER_SCHEDULING_TRANSITIONS.get(currentSchedulingStatus);
        if (allowedTransitions == null) {
            log.warn("运单调度状态校验失败：当前状态[{}]不存在或为终态", currentSchedulingStatus);
            return false;
        }

        boolean allowed = allowedTransitions.contains(targetSchedulingStatus);
        if (!allowed) {
            log.warn("运单调度状态流转非法：当前状态[{}]不能流转到目标状态[{}]",
                TransportOrderSchedulingStatus.lookup(currentSchedulingStatus),
                TransportOrderSchedulingStatus.lookup(targetSchedulingStatus));
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
     * 获取运单-状态(status)维度允许的下一个状态
     *
     * @param currentStatus 当前状态（TransportOrderStatus.code）
     * @return 允许的下一个状态集合
     */
    public Set<Integer> getAllowedNextTransportOrderStatuses(Integer currentStatus) {
        return TRANSPORT_ORDER_STATUS_TRANSITIONS.getOrDefault(currentStatus, Collections.emptySet());
    }

    /**
     * 获取运单-调度状态(schedulingStatus)维度允许的下一个状态
     *
     * @param currentSchedulingStatus 当前调度状态（TransportOrderSchedulingStatus.code）
     * @return 允许的下一个状态集合
     */
    public Set<Integer> getAllowedNextTransportOrderSchedulingStatuses(Integer currentSchedulingStatus) {
        return TRANSPORT_ORDER_SCHEDULING_TRANSITIONS.getOrDefault(currentSchedulingStatus, Collections.emptySet());
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
