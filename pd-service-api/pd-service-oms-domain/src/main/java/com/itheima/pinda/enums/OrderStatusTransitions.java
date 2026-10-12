package com.itheima.pinda.enums;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 订单状态流转规则（单一事实源）
 *
 * <p>订单状态流转图唯一定义处，pd-work 的 {@code StateTransitionValidator} 与
 * pd-oms 的 {@code OrderServiceImpl} 均委托本类校验，杜绝双份副本漂移。
 * 修改流转规则只改本类，两侧立即同步生效。</p>
 *
 * <p>流转规则（与 OrderStatus 紧凑码 1~12 一致）：</p>
 * <ul>
 *   <li>待取件(1) → 已取件(2) / 已取消(12)</li>
 *   <li>已取件(2) → 网点入库(4) / 已取消(12)</li>
 *   <li>网点自寄(3) → 网点入库(4) / 已取消(12)（自寄订单交件直接入库）</li>
 *   <li>网点入库(4) → 待装车(5) / 已取消(12)</li>
 *   <li>待装车(5) → 运输中(6) / 已取消(12)</li>
 *   <li>运输中(6) → 网点出库(7) / 已取消(12)</li>
 *   <li>网点出库(7) → 待派送(8) / 派送中(9) / 已取消(12)（快递员接件可直接进入派送，兼容跳过待派送）</li>
 *   <li>待派送(8) → 派送中(9) / 已取消(12)</li>
 *   <li>派送中(9) → 已签收(10) / 拒收(11) / 已取消(12)</li>
 *   <li>已签收(10)、拒收(11)、已取消(12) → 终态，不能流转</li>
 * </ul>
 *
 * @author Claude Code
 * @since 2026-10-12
 */
public final class OrderStatusTransitions {

    /**
     * 订单状态流转图（不可变）
     * Key: 当前状态 code；Value: 允许的下一个状态集合（不可变，终态为空集）
     */
    private static final Map<Integer, Set<Integer>> TRANSITIONS;

    static {
        Map<Integer, Set<Integer>> map = new HashMap<>();

        // 待取件 → 已取件 / 已取消
        map.put(OrderStatus.PENDING.getCode(), immutableOf(
            OrderStatus.PICKED_UP.getCode(),
            OrderStatus.CANCELLED.getCode()
        ));

        // 已取件 → 网点入库 / 已取消
        map.put(OrderStatus.PICKED_UP.getCode(), immutableOf(
            OrderStatus.OUTLETS_WAREHOUSE.getCode(),
            OrderStatus.CANCELLED.getCode()
        ));

        // 网点自寄 → 网点入库 / 已取消（自寄订单交件直接入库）
        map.put(OrderStatus.OUTLETS_SINCE_SENT.getCode(), immutableOf(
            OrderStatus.OUTLETS_WAREHOUSE.getCode(),
            OrderStatus.CANCELLED.getCode()
        ));

        // 网点入库 → 待装车 / 已取消
        map.put(OrderStatus.OUTLETS_WAREHOUSE.getCode(), immutableOf(
            OrderStatus.FOR_LOADING.getCode(),
            OrderStatus.CANCELLED.getCode()
        ));

        // 待装车 → 运输中 / 已取消
        map.put(OrderStatus.FOR_LOADING.getCode(), immutableOf(
            OrderStatus.IN_TRANSIT.getCode(),
            OrderStatus.CANCELLED.getCode()
        ));

        // 运输中 → 网点出库 / 已取消
        map.put(OrderStatus.IN_TRANSIT.getCode(), immutableOf(
            OrderStatus.OUTLETS_EX_WAREHOUSE.getCode(),
            OrderStatus.CANCELLED.getCode()
        ));

        // 网点出库 → 待派送 / 派送中 / 已取消（快递员接件可直接进入派送，兼容跳过待派送）
        map.put(OrderStatus.OUTLETS_EX_WAREHOUSE.getCode(), immutableOf(
            OrderStatus.TO_BE_DISPATCHED.getCode(),
            OrderStatus.DISPATCHING.getCode(),
            OrderStatus.CANCELLED.getCode()
        ));

        // 待派送 → 派送中 / 已取消
        map.put(OrderStatus.TO_BE_DISPATCHED.getCode(), immutableOf(
            OrderStatus.DISPATCHING.getCode(),
            OrderStatus.CANCELLED.getCode()
        ));

        // 派送中 → 已签收 / 拒收 / 已取消
        map.put(OrderStatus.DISPATCHING.getCode(), immutableOf(
            OrderStatus.RECEIVED.getCode(),
            OrderStatus.REJECTION.getCode(),
            OrderStatus.CANCELLED.getCode()
        ));

        // 已签收、拒收、已取消 → 终态，不能流转
        map.put(OrderStatus.RECEIVED.getCode(), Collections.emptySet());
        map.put(OrderStatus.REJECTION.getCode(), Collections.emptySet());
        map.put(OrderStatus.CANCELLED.getCode(), Collections.emptySet());

        TRANSITIONS = Collections.unmodifiableMap(map);
    }

    private OrderStatusTransitions() {
    }

    /**
     * 构建不可变状态集合
     *
     * @param codes 允许的下一个状态 code
     * @return 不可变集合
     */
    private static Set<Integer> immutableOf(Integer... codes) {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(codes)));
    }

    /**
     * 校验订单状态流转是否合法
     *
     * @param currentStatus 当前状态 code
     * @param targetStatus 目标状态 code
     * @return 是否允许流转（任一状态为 null 或当前状态不在流转图中时返回 false）
     */
    public static boolean canTransition(Integer currentStatus, Integer targetStatus) {
        if (currentStatus == null || targetStatus == null) {
            return false;
        }
        Set<Integer> allowed = TRANSITIONS.get(currentStatus);
        return allowed != null && allowed.contains(targetStatus);
    }

    /**
     * 获取当前状态允许流转的下一个状态集合
     *
     * @param currentStatus 当前状态 code
     * @return 允许的下一个状态集合（终态为空集；未知状态返回空集），不可变
     */
    public static Set<Integer> allowedNextStatuses(Integer currentStatus) {
        if (currentStatus == null) {
            return Collections.emptySet();
        }
        return TRANSITIONS.getOrDefault(currentStatus, Collections.emptySet());
    }
}
