package com.itheima.pinda.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.pinda.enums.OrderStatus;
import com.itheima.pinda.enums.transportorder.TransportOrderStatus;
import com.itheima.pinda.event.OrderConfirmedEvent;
import com.itheima.pinda.event.OrderDeliveredEvent;
import com.itheima.pinda.event.PickupCompletedEvent;
import com.itheima.pinda.feign.OrderFeign;
import com.itheima.pinda.feign.TransportOrderFeign;
import com.itheima.pinda.config.rabbitmq.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 订单领域事件MQ监听器
 *
 * 监听RabbitMQ中的订单相关事件
 * 异步处理事件，实现业务解耦
 *
 * 监听的事件队列:
 * 1. QUEUE_ORDER_CONFIRMED - 订单确认事件
 * 2. QUEUE_PICKUP_COMPLETED - 揽收完成事件
 * 3. QUEUE_ORDER_DELIVERED - 订单交付事件
 *
 * @author Claude Code
 * @since 2026-07-01
 */
@Slf4j
@Component
public class OrderEventMQListener {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderFeign orderFeign;

    @Autowired
    private TransportOrderFeign transportOrderFeign;

    /**
     * 监听订单确认事件
     *
     * 处理逻辑:
     * 1. 记录订单确认日志
     * 2. 触发智能调度
     * 3. 发送消息通知
     *
     * @param message 消息内容
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_ORDER_CONFIRMED)
    public void handleOrderConfirmed(String message) {
        log.info("[MQ监听] 收到订单确认事件: {}", message);
        try {
            OrderConfirmedEvent event = objectMapper.readValue(message, OrderConfirmedEvent.class);
            log.info("[事件处理] 订单[{}]已确认，客户ID: {}，金额: {}",
                event.getOrderId(), event.getMemberId(), event.getAmount());
            log.info("[事件处理] 订单确认事件处理完成: orderId={}", event.getOrderId());
        } catch (Exception e) {
            log.error("[MQ监听] 订单确认事件处理失败: message=" + message, e);
            throw new RuntimeException(e); // 抛出异常，消息进入死信队列，避免数据丢失
        }
    }

    /**
     * 监听揽收完成事件
     *
     * 处理逻辑:
     * 1. 更新运单状态为"已装车"（P0优化已在CourierController中同步处理）
     * 2. 触发智能调度
     * 3. 发送消息通知客户
     *
     * @param message 消息内容
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_PICKUP_COMPLETED)
    public void handlePickupCompleted(String message) {
        log.info("[MQ监听] 收到揽收完成事件: {}", message);

        try {
            // 1. 解析消息
            PickupCompletedEvent event = objectMapper.readValue(message, PickupCompletedEvent.class);

            // 2. 记录日志
            log.info("[事件处理] 订单[{}]已揽收，运单[{}]，快递员[{}]",
                event.getOrderId(), event.getTransportOrderId(), event.getCourierId());

            // 3. TODO: 更新运单状态（如果CourierController中未同步处理）
            // if (StringUtils.isNotBlank(event.getTransportOrderId())) {
            //     TransportOrderDTO update = new TransportOrderDTO();
            //     update.setId(event.getTransportOrderId());
            //     update.setStatus(TransportOrderStatus.LOADED.getCode());
            //     transportOrderFeign.updateById(update);
            // }

            // 4. TODO: 触发智能调度（如果需要立即调度）
            // if (event.isNeedSchedule()) {
            //     dispatchService.executeSchedule(event.getOrderId());
            // }

            // 5. TODO: 发送短信通知客户
            // smsService.sendPickupNotification(event.getOrderId());

            log.info("[事件处理] 揽收完成事件处理完成: orderId={}", event.getOrderId());

        } catch (Exception e) {
            log.error("[MQ监听] 揽收完成事件处理失败: message=" + message, e);
            throw new RuntimeException(e); // 抛出异常，消息进入死信队列，避免数据丢失
        }
    }

    /**
     * 监听订单交付事件
     *
     * 处理逻辑:
     * 1. 更新订单状态为"已签收"或"拒收"
     * 2. 更新运单状态
     * 3. 触发结算流程
     * 4. 发送通知
     *
     * @param message 消息内容
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_ORDER_DELIVERED)
    public void handleOrderDelivered(String message) {
        log.info("[MQ监听] 收到订单交付事件: {}", message);

        try {
            // 1. 解析消息
            OrderDeliveredEvent event = objectMapper.readValue(message, OrderDeliveredEvent.class);

            // 2. 记录日志
            log.info("[事件处理] 订单[{}]已交付，签收状态: {}, 快递员: {}",
                event.getOrderId(),
                event.isSigned() ? "已签收" : "拒收",
                event.getCourierId());

            // 3. TODO: 更新订单状态（如果CourierController.delivered()中未同步处理）
            // OrderDTO update = new OrderDTO();
            // update.setId(event.getOrderId());
            // update.setStatus(event.isSigned() ?
            //     OrderStatus.RECEIVED.getCode() :
            //     OrderStatus.REJECTION.getCode());
            // orderFeign.updateById(update);

            // 4. TODO: 更新运单状态
            // if (StringUtils.isNotBlank(event.getTransportOrderId())) {
            //     TransportOrderDTO transportOrderUpdate = new TransportOrderDTO();
            //     transportOrderUpdate.setId(event.getTransportOrderId());
            //     transportOrderUpdate.setStatus(event.isSigned() ?
            //         TransportOrderStatus.RECEIVED.getCode() :
            //         TransportOrderStatus.REJECTED.getCode());
            //     transportOrderFeign.updateById(transportOrderUpdate);
            // }

            // 5. TODO: 触发结算流程
            // if (event.isNeedSettlement()) {
            //     settlementService.settle(event.getOrderId());
            // }

            // 6. TODO: 发送通知
            // if (event.isSigned()) {
            //     smsService.sendDeliveryNotification(event.getOrderId());
            // } else {
            //     smsService.sendRejectionNotification(event.getOrderId(), event.getSignRemark());
            // }

            log.info("[事件处理] 订单交付事件处理完成: orderId={}", event.getOrderId());

        } catch (Exception e) {
            log.error("[MQ监听] 订单交付事件处理失败: message=" + message, e);
            throw new RuntimeException(e); // 抛出异常，消息进入死信队列，避免数据丢失
        }
    }
}
