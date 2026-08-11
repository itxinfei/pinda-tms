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
import com.itheima.pinda.service.SmsNotificationService;
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

    @Autowired
    private SmsNotificationService smsNotificationService;

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

            // 发送订单确认短信通知
            sendOrderSms(event.getOrderId(), "您的寄件订单[" + event.getOrderNo() + "]已确认，快递员将尽快上门取件。");

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

            // 3. 更新运单状态（如果CourierController中未同步处理）
            // if (StringUtils.isNotBlank(event.getTransportOrderId())) {
            //     TransportOrderDTO update = new TransportOrderDTO();
            //     update.setId(event.getTransportOrderId());
            //     update.setStatus(TransportOrderStatus.LOADED.getCode());
            //     transportOrderFeign.updateById(update);
            // }

            // 4. 触发智能调度（如果需要立即调度）
            // if (event.isNeedSchedule()) {
            //     dispatchService.executeSchedule(event.getOrderId());
            // }

            // 5. 发送揽收完成短信通知
            sendOrderSms(event.getOrderId(), "您的快件已被快递员揽收，正在运送途中。");

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

            // 3. 更新订单状态（如果CourierController.delivered()中未同步处理）
            // OrderDTO update = new OrderDTO();
            // update.setId(event.getOrderId());
            // update.setStatus(event.isSigned() ?
            //     OrderStatus.RECEIVED.getCode() :
            //     OrderStatus.REJECTION.getCode());
            // orderFeign.updateById(update);

            // 4. 更新运单状态
            // if (StringUtils.isNotBlank(event.getTransportOrderId())) {
            //     TransportOrderDTO transportOrderUpdate = new TransportOrderDTO();
            //     transportOrderUpdate.setId(event.getTransportOrderId());
            //     transportOrderUpdate.setStatus(event.isSigned() ?
            //         TransportOrderStatus.RECEIVED.getCode() :
            //         TransportOrderStatus.REJECTED.getCode());
            //     transportOrderFeign.updateById(transportOrderUpdate);
            // }

            // 5. 触发结算流程
            // if (event.isNeedSettlement()) {
            //     settlementService.settle(event.getOrderId());
            // }

            // 6. 发送妥投/拒收短信通知
            if (event.isSigned()) {
                sendOrderSms(event.getOrderId(), "您的快件已被签收，感谢使用品达物流。");
            } else {
                sendOrderSms(event.getOrderId(), "您的快件因未能送达，如有疑问请联系客服。");
            }

            log.info("[事件处理] 订单交付事件处理完成: orderId={}", event.getOrderId());

        } catch (Exception e) {
            log.error("[MQ监听] 订单交付事件处理失败: message=" + message, e);
            throw new RuntimeException(e); // 抛出异常，消息进入死信队列，避免数据丢失
        }
    }

    /**
     * 根据订单号查询收件人手机号并发送短信通知
     *
     * @param orderId 订单ID
     * @param content 短信内容
     */
    private void sendOrderSms(String orderId, String content) {
        try {
            if (org.apache.commons.lang.StringUtils.isBlank(orderId)) {
                log.warn("[短信通知] 订单ID为空，跳过短信发送");
                return;
            }
            com.itheima.pinda.DTO.OrderDTO orderDTO = orderFeign.findById(orderId);
            if (orderDTO == null) {
                log.warn("[短信通知] 订单[{}]不存在，跳过短信发送", orderId);
                return;
            }
            String mobile = org.apache.commons.lang.StringUtils.isNotBlank(orderDTO.getReceiverPhone())
                ? orderDTO.getReceiverPhone() : orderDTO.getSenderPhone();
            if (org.apache.commons.lang.StringUtils.isBlank(mobile)) {
                log.warn("[短信通知] 订单[{}]无收件人/发件人手机号，跳过短信发送", orderId);
                return;
            }
            smsNotificationService.sendSms(mobile, content);
        } catch (Exception e) {
            // 通知失败不影响业务主流程
            log.error("[短信通知] 订单[{}]短信发送异常", orderId, e);
        }
    }
}
