package com.itheima.pinda.event;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 订单交付完成事件
 *
 * 触发时机: 快递员完成派送，客户签收或拒收
 * 事件标识: ORDER_DELIVERED
 *
 * @author Claude Code
 * @since 2026-07-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderDeliveredEvent extends DomainEvent {

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
     * 是否签收 (true=签收, false=拒收)
     */
    private boolean signed;

    /**
     * 签收备注
     */
    private String signRemark;

    /**
     * 派送任务ID
     */
    private String dispatchTaskId;

    /**
     * 快递员ID
     */
    private String courierId;

    /**
     * 是否需要触发结算
     */
    private boolean needSettlement = true;

    public OrderDeliveredEvent() {
        super("ORDER_DELIVERED");
    }

    public OrderDeliveredEvent(String orderId, String transportOrderId,
                               boolean signed, String signRemark,
                               String dispatchTaskId, String courierId) {
        super("ORDER_DELIVERED");
        this.orderId = orderId;
        this.transportOrderId = transportOrderId;
        this.signed = signed;
        this.signRemark = signRemark;
        this.dispatchTaskId = dispatchTaskId;
        this.courierId = courierId;
    }
}
