package com.itheima.pinda.listener;

import com.itheima.pinda.DTO.TransportOrderDTO;
import com.itheima.pinda.event.OrderConfirmedEvent;
import com.itheima.pinda.event.OrderDeliveredEvent;
import com.itheima.pinda.event.PickupCompletedEvent;
import com.itheima.pinda.enums.OrderStatus;
import com.itheima.pinda.enums.transportorder.TransportOrderStatus;
import com.itheima.pinda.feign.OrderFeign;
import com.itheima.pinda.feign.TransportOrderFeign;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 订单事件监听器
 *
 * 监听订单相关的领域事件，实现业务解耦
 *
 * 监听的事件:
 * 1. OrderConfirmedEvent - 订单确认事件
 * 2. PickupCompletedEvent - 揽收完成事件
 * 3. OrderDeliveredEvent - 订单交付事件
 *
 * @author Claude Code
 * @since 2026-07-01
 */
@Slf4j
@Component
public class OrderEventListener {

    @Autowired
    private OrderFeign orderFeign;

    @Autowired
    private TransportOrderFeign transportOrderFeign;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    /**
     * 监听订单确认事件
     *
     * 异步处理，不阻塞下单流程
     * 处理逻辑:
     * 1. 记录订单确认日志
     * 2. 可以在这里添加额外的业务逻辑，比如:
     *    - 发送短信通知客户
     *    - 推送消息到消息队列
     *    - 触发预调度计算
     *
     * 注意: 由于P0优化中已经在MailingController.save()中同步创建了运单
     * 这里主要处理额外的异步业务逻辑
     *
     * @param event 订单确认事件
     */
    @Async
    @EventListener
    public void handleOrderConfirmed(OrderConfirmedEvent event) {
        log.info("[事件监听] 订单确认事件触发: orderId={}, needPreSchedule={}",
            event.getOrderId(), event.isNeedPreSchedule());

        // 1. 记录订单确认日志
        log.info("[事件处理] 订单[{}]已确认，客户ID: {}，金额: {}",
            event.getOrderId(),
            event.getMemberId(),
            event.getAmount());

        // 2. TODO: 发送短信通知客户
        // smsService.sendOrderConfirmation(event.getOrderId());

        // 3. TODO: 推送消息到消息队列，供其他服务消费
        // eventPublisher.publishEvent(new OrderMessageEvent(this, event.getOrderId()));

        // 4. TODO: 触发预调度计算（如果需要）
        // if (event.isNeedPreSchedule()) {
        //     dispatchService.preSchedule(event.getOrderId());
        // }

        log.info("[事件处理] 订单确认事件处理完成: orderId={}", event.getOrderId());
    }

    /**
     * 监听揽收完成事件
     *
     * 异步处理，不阻塞揽收流程
     * 处理逻辑:
     * 1. 更新运单状态为"已装车"（P0优化已在CourierController中同步处理）
     * 2. 触发智能调度(执行调度)
     * 3. 发送通知给客户
     *
     * @param event 揽收完成事件
     */
    @Async
    @EventListener
    public void handlePickupCompleted(PickupCompletedEvent event) {
        log.info("[事件监听] 揽收完成事件触发: orderId={}, transportOrderId={}, courierId={}",
            event.getOrderId(), event.getTransportOrderId(), event.getCourierId());

        // 1. 记录揽收完成日志
        log.info("[事件处理] 订单[{}]已揽收，运单[{}]，快递员[{}]",
            event.getOrderId(), event.getTransportOrderId(), event.getCourierId());

        // 2. TODO: 触发智能调度(执行调度)
        // 注意: P0优化中，揽收时已更新运单状态为"已装车"
        // 如果需要立即触发调度，可以在这里调用:
        // if (event.isNeedSchedule()) {
        //     dispatchService.executeSchedule(event.getOrderId());
        // }

        // 3. TODO: 发送短信通知客户
        // smsService.sendPickupNotification(event.getOrderId());

        // 4. TODO: 推送消息到消息队列
        // eventPublisher.publishEvent(new PickupMessageEvent(this, event.getOrderId()));

        log.info("[事件处理] 揽收完成事件处理完成: orderId={}", event.getOrderId());
    }

    /**
     * 监听订单交付完成事件
     *
     * 异步处理，不阻塞交付流程
     * 处理逻辑:
     * 1. 更新订单状态为"已签收"或"拒收"
     * 2. 更新运单状态
     * 3. 触发结算流程
     * 4. 发送通知
     *
     * @param event 订单交付事件
     */
    @Async
    @EventListener
    public void handleOrderDelivered(OrderDeliveredEvent event) {
        log.info("[事件监听] 订单交付事件触发: orderId={}, signed={}, courierId={}",
            event.getOrderId(), event.isSigned(), event.getCourierId());

        // 1. 更新订单状态
        // 注意: P0优化中，CourierController.delivered()已同步更新订单状态
        // 这里主要处理额外的业务逻辑
        log.info("[事件处理] 订单[{}]已交付，签收状态: {}, 备注: {}",
            event.getOrderId(),
            event.isSigned() ? "已签收" : "拒收",
            event.getSignRemark());

        // 2. TODO: 更新运单状态
        // if (StringUtils.isNotBlank(event.getTransportOrderId())) {
        //     TransportOrderDTO update = new TransportOrderDTO();
        //     update.setId(event.getTransportOrderId());
        //     update.setStatus(event.isSigned() ?
        //         TransportOrderStatus.RECEIVED.getCode() :
        //         TransportOrderStatus.REJECTED.getCode());
        //     transportOrderFeign.updateById(update);
        // }

        // 3. TODO: 触发结算流程
        // if (event.isNeedSettlement()) {
        //     settlementService.settle(event.getOrderId());
        // }

        // 4. TODO: 发送通知
        // if (event.isSigned()) {
        //     smsService.sendDeliveryNotification(event.getOrderId());
        // } else {
        //     smsService.sendRejectionNotification(event.getOrderId(), event.getSignRemark());
        // }

        // 5. TODO: 推送消息到消息队列
        // eventPublisher.publishEvent(new DeliveryMessageEvent(this, event.getOrderId()));

        log.info("[事件处理] 订单交付事件处理完成: orderId={}", event.getOrderId());
    }

    /**
     * 监听运输任务完成事件（预留）
     *
     * 用于处理运输任务完成后的额外业务逻辑
     *
     * @param event 运输任务完成事件
     */
    // @Async
    // @EventListener
    // public void handleTransportTaskCompleted(TransportTaskCompletedEvent event) {
    //     log.info("[事件监听] 运输任务完成事件触发: taskId={}", event.getTaskId());
    //     // TODO: 处理运输任务完成后的业务逻辑
    // }
}
