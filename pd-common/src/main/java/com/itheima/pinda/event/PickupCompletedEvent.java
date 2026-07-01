package com.itheima.pinda.event;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 揽收完成事件
 *
 * 触发时机: 快递员完成揽收，确认取件
 * 事件标识: PICKUP_COMPLETED
 *
 * @author Claude Code
 * @since 2026-07-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PickupCompletedEvent extends DomainEvent {

    private static final long serialVersionUID = 1L;

    /**
     * 订单ID
     */
    private String orderId;

    /**
     * 运单ID
     */
    private String transportOrderId;

    /**
     * 快递员ID
     */
    private String courierId;

    /**
     * 取派件任务ID
     */
    private String pickupTaskId;

    /**
     * 揽收时间
     */
    private String pickupTime;

    /**
     * 是否需要立即调度
     */
    private boolean needSchedule = true;

    public PickupCompletedEvent() {
        super("PICKUP_COMPLETED");
    }

    public PickupCompletedEvent(String orderId, String transportOrderId,
                                String courierId, String pickupTaskId, String pickupTime) {
        super("PICKUP_COMPLETED");
        this.orderId = orderId;
        this.transportOrderId = transportOrderId;
        this.courierId = courierId;
        this.pickupTaskId = pickupTaskId;
        this.pickupTime = pickupTime;
    }
}
