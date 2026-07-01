package com.itheima.pinda.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.pinda.event.DomainEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 领域事件发布器
 *
 * 使用RabbitMQ发送领域事件
 * 支持异步处理，提高系统响应速度
 *
 * @author Claude Code
 * @since 2026-07-01
 */
@Slf4j
@Component
public class EventPublisher {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 交换机名称
     */
    public static final String EXCHANGE_DOMAIN_EVENT = "pinda.domain.event.exchange";

    /**
     * 发布领域事件
     *
     * @param event 领域事件
     * @param <T> 事件类型
     */
    public <T extends DomainEvent> void publish(T event) {
        try {
            String routingKey = event.getEventType();
            String message = objectMapper.writeValueAsString(event);

            log.info("[事件发布] 发布事件: type={}, eventId={}, routingKey={}",
                event.getEventType(), event.getEventId(), routingKey);

            rabbitTemplate.convertAndSend(EXCHANGE_DOMAIN_EVENT, routingKey, message);

            log.info("[事件发布] 事件发布成功: eventId={}", event.getEventId());
        } catch (Exception e) {
            log.error("[事件发布] 事件发布失败: eventId=" + event.getEventId(), e);
            // TODO: 添加失败重试机制或死信队列
        }
    }

    /**
     * 发布订单确认事件
     *
     * @param event 订单确认事件
     */
    public void publishOrderConfirmed(com.itheima.pinda.event.OrderConfirmedEvent event) {
        publish((DomainEvent) event);
    }

    /**
     * 发布揽收完成事件
     *
     * @param event 揽收完成事件
     */
    public void publishPickupCompleted(com.itheima.pinda.event.PickupCompletedEvent event) {
        publish((DomainEvent) event);
    }

    /**
     * 发布订单交付事件
     *
     * @param event 订单交付事件
     */
    public void publishOrderDelivered(com.itheima.pinda.event.OrderDeliveredEvent event) {
        publish((DomainEvent) event);
    }
}
