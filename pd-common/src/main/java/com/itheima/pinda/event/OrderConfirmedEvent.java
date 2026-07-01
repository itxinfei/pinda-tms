package com.itheima.pinda.event;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 订单确认事件
 *
 * 触发时机: 客户下单成功后
 * 事件标识: ORDER_CONFIRMED
 *
 * @author Claude Code
 * @since 2026-07-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderConfirmedEvent extends DomainEvent {

    private static final long serialVersionUID = 1L;

    /**
     * 订单ID
     */
    private String orderId;

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 客户ID
     */
    private String memberId;

    /**
     * 订单金额
     */
    private BigDecimal amount;

    /**
     * 发货地址
     */
    private String senderAddress;

    /**
     * 收货地址
     */
    private String receiverAddress;

    /**
     * 是否需要预调度
     */
    private boolean needPreSchedule = true;

    public OrderConfirmedEvent() {
        super("ORDER_CONFIRMED");
    }

    public OrderConfirmedEvent(String orderId, String orderNo, String memberId,
                                BigDecimal amount, String senderAddress, String receiverAddress) {
        super("ORDER_CONFIRMED");
        this.orderId = orderId;
        this.orderNo = orderNo;
        this.memberId = memberId;
        this.amount = amount;
        this.senderAddress = senderAddress;
        this.receiverAddress = receiverAddress;
    }
}
