package com.itheima.pinda.state;

import com.itheima.pinda.enums.OrderStatus;
import com.itheima.pinda.enums.transportorder.TransportOrderSchedulingStatus;
import com.itheima.pinda.enums.transportorder.TransportOrderStatus;
import com.itheima.pinda.enums.transporttask.TransportTaskStatus;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * 状态机流转校验单元测试
 *
 * <p>覆盖订单/运单(状态+调度)/运输任务四张流转图的关键路径与非法跳变拦截。</p>
 */
public class StateTransitionValidatorTest {

    private StateTransitionValidator validator;

    @Before
    public void setUp() {
        validator = new StateTransitionValidator();
    }

    // ==================== 订单状态机 ====================

    @Test
    public void testOrderNormalFlow() {
        // 待取件 → 已取件
        assertTrue(validator.validateOrderStatusTransition(
            OrderStatus.PENDING.getCode(), OrderStatus.PICKED_UP.getCode()));
        // 已取件 → 网点入库
        assertTrue(validator.validateOrderStatusTransition(
            OrderStatus.PICKED_UP.getCode(), OrderStatus.OUTLETS_WAREHOUSE.getCode()));
        // 网点自寄 → 网点入库（自寄交件）
        assertTrue(validator.validateOrderStatusTransition(
            OrderStatus.OUTLETS_SINCE_SENT.getCode(), OrderStatus.OUTLETS_WAREHOUSE.getCode()));
        // 网点入库 → 待装车
        assertTrue(validator.validateOrderStatusTransition(
            OrderStatus.OUTLETS_WAREHOUSE.getCode(), OrderStatus.FOR_LOADING.getCode()));
        // 待装车 → 运输中
        assertTrue(validator.validateOrderStatusTransition(
            OrderStatus.FOR_LOADING.getCode(), OrderStatus.IN_TRANSIT.getCode()));
        // 运输中 → 网点出库
        assertTrue(validator.validateOrderStatusTransition(
            OrderStatus.IN_TRANSIT.getCode(), OrderStatus.OUTLETS_EX_WAREHOUSE.getCode()));
        // 网点出库 → 派送中（接件直接进入派送）
        assertTrue(validator.validateOrderStatusTransition(
            OrderStatus.OUTLETS_EX_WAREHOUSE.getCode(), OrderStatus.DISPATCHING.getCode()));
        // 派送中 → 已签收 / 拒收
        assertTrue(validator.validateOrderStatusTransition(
            OrderStatus.DISPATCHING.getCode(), OrderStatus.RECEIVED.getCode()));
        assertTrue(validator.validateOrderStatusTransition(
            OrderStatus.DISPATCHING.getCode(), OrderStatus.REJECTION.getCode()));
    }

    @Test
    public void testOrderIllegalJumpRejected() {
        // 待取件 → 网点入库：跳变，应拦截
        assertFalse(validator.validateOrderStatusTransition(
            OrderStatus.PENDING.getCode(), OrderStatus.OUTLETS_WAREHOUSE.getCode()));
        // 已取件 → 派送中：跳变，应拦截
        assertFalse(validator.validateOrderStatusTransition(
            OrderStatus.PICKED_UP.getCode(), OrderStatus.DISPATCHING.getCode()));
        // 待装车 → 已签收：跳变，应拦截
        assertFalse(validator.validateOrderStatusTransition(
            OrderStatus.FOR_LOADING.getCode(), OrderStatus.RECEIVED.getCode()));
    }

    @Test
    public void testOrderTerminalStatesLocked() {
        // 终态（已签收/拒收/已取消）不允许再流转
        assertFalse(validator.validateOrderStatusTransition(
            OrderStatus.RECEIVED.getCode(), OrderStatus.DISPATCHING.getCode()));
        assertFalse(validator.validateOrderStatusTransition(
            OrderStatus.REJECTION.getCode(), OrderStatus.PICKED_UP.getCode()));
        assertFalse(validator.validateOrderStatusTransition(
            OrderStatus.CANCELLED.getCode(), OrderStatus.PENDING.getCode()));
    }

    @Test
    public void testOrderNullStatusRejected() {
        assertFalse(validator.validateOrderStatusTransition(null, OrderStatus.PICKED_UP.getCode()));
        assertFalse(validator.validateOrderStatusTransition(OrderStatus.PENDING.getCode(), null));
    }

    // ==================== 运单状态机 ====================

    @Test
    public void testTransportOrderNormalFlow() {
        // 新建 → 已装车 → 到达 → 到达终端网点 → 已签收
        assertTrue(validator.validateTransportOrderStatusTransition(
            TransportOrderStatus.CREATED.getCode(), TransportOrderStatus.LOADED.getCode()));
        assertTrue(validator.validateTransportOrderStatusTransition(
            TransportOrderStatus.LOADED.getCode(), TransportOrderStatus.ARRIVED.getCode()));
        assertTrue(validator.validateTransportOrderStatusTransition(
            TransportOrderStatus.LOADED.getCode(), TransportOrderStatus.ARRIVED_END.getCode()));
        assertTrue(validator.validateTransportOrderStatusTransition(
            TransportOrderStatus.ARRIVED.getCode(), TransportOrderStatus.ARRIVED_END.getCode()));
        assertTrue(validator.validateTransportOrderStatusTransition(
            TransportOrderStatus.ARRIVED_END.getCode(), TransportOrderStatus.RECEIVED.getCode()));
    }

    @Test
    public void testTransportOrderIllegalFlowRejected() {
        // 新建 → 到达：跳变（必须先装车），应拦截
        assertFalse(validator.validateTransportOrderStatusTransition(
            TransportOrderStatus.CREATED.getCode(), TransportOrderStatus.ARRIVED.getCode()));
        // 已签收 → 拒收：终态互转，应拦截
        assertFalse(validator.validateTransportOrderStatusTransition(
            TransportOrderStatus.RECEIVED.getCode(), TransportOrderStatus.REJECTED.getCode()));
        // 到达 → 已签收：跳变（必须先到达终端网点），应拦截
        assertFalse(validator.validateTransportOrderStatusTransition(
            TransportOrderStatus.ARRIVED.getCode(), TransportOrderStatus.RECEIVED.getCode()));
    }

    // ==================== 运单调度状态机 ====================

    @Test
    public void testTransportOrderSchedulingFlow() {
        // 待调度 → 已调度
        assertTrue(validator.validateTransportOrderSchedulingTransition(
            TransportOrderSchedulingStatus.TO_BE_SCHEDULED.getCode(),
            TransportOrderSchedulingStatus.SCHEDULED.getCode()));
        // 待调度 → 未匹配到线路：不允许（待调度只能直接调度成功）
        assertFalse(validator.validateTransportOrderSchedulingTransition(
            TransportOrderSchedulingStatus.TO_BE_SCHEDULED.getCode(),
            TransportOrderSchedulingStatus.NO_MATCH_TRANSPORTLINE.getCode()));
        // 未匹配到线路 → 已调度（允许重新调度）
        assertTrue(validator.validateTransportOrderSchedulingTransition(
            TransportOrderSchedulingStatus.NO_MATCH_TRANSPORTLINE.getCode(),
            TransportOrderSchedulingStatus.SCHEDULED.getCode()));
        // 已调度 → 待调度：回退，应拦截
        assertFalse(validator.validateTransportOrderSchedulingTransition(
            TransportOrderSchedulingStatus.SCHEDULED.getCode(),
            TransportOrderSchedulingStatus.TO_BE_SCHEDULED.getCode()));
    }

    // ==================== 运输任务状态机 ====================

    @Test
    public void testTransportTaskFlow() {
        // 待执行 → 进行中 → 待确认 → 已完成
        assertTrue(validator.validateTransportTaskTransition(
            TransportTaskStatus.PENDING.getCode(), TransportTaskStatus.PROCESSING.getCode()));
        assertTrue(validator.validateTransportTaskTransition(
            TransportTaskStatus.PROCESSING.getCode(), TransportTaskStatus.CONFIRM.getCode()));
        assertTrue(validator.validateTransportTaskTransition(
            TransportTaskStatus.CONFIRM.getCode(), TransportTaskStatus.COMPLETED.getCode()));
        // 已完成 → 已取消：终态互转，应拦截
        assertFalse(validator.validateTransportTaskTransition(
            TransportTaskStatus.COMPLETED.getCode(), TransportTaskStatus.CANCELLED.getCode()));
    }

    // ==================== 允许的下一个状态查询 ====================

    @Test
    public void testGetAllowedNextOrderStatuses() {
        Set<Integer> allowed = validator.getAllowedNextOrderStatuses(OrderStatus.PENDING.getCode());
        assertNotNull(allowed);
        assertTrue(allowed.contains(OrderStatus.PICKED_UP.getCode()));
        assertTrue(allowed.contains(OrderStatus.CANCELLED.getCode()));
        assertFalse(allowed.contains(OrderStatus.RECEIVED.getCode()));
    }

    @Test
    public void testGetAllowedNextUnknownStatus() {
        // 未知状态返回空集合而非异常
        Set<Integer> allowed = validator.getAllowedNextOrderStatuses(999999);
        assertNotNull(allowed);
        assertEquals(0, allowed.size());
    }
}
