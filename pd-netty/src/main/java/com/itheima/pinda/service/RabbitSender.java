package com.itheima.pinda.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 用于操作RabbitMQ（替代原KafkaSender）
 * 所有业务事件 + 轨迹数据都走RabbitMQ
 */
@Slf4j
@Component
public class RabbitSender {

    public final static String GPS_EXCHANGE = "pinda.gps.exchange";//GPS轨迹交换机
    public final static String GPS_QUEUE = "pinda.gps.queue";//GPS轨迹队列
    public final static String GPS_ROUTING_KEY = "gps.#";//GPS轨迹路由键

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 向RabbitMQ队列发送GPS轨迹消息
     * 用truckId作为routingKey，保证同一辆车的轨迹串行消费，不会乱序
     *
     * @param truckId 车辆ID，作为路由键的一部分
     * @param message GPS轨迹消息
     */
    public void sendGpsTrace(String truckId, String message) {
        try {
            String routingKey = "gps." + truckId;
            rabbitTemplate.convertAndSend(GPS_EXCHANGE, routingKey, message);
            log.debug("RabbitMQ GPS轨迹消息发送成功: truckId={}", truckId);
        } catch (Exception e) {
            log.error("发送RabbitMQ GPS轨迹消息失败: truckId={}, 消息将被丢弃", truckId, e);
        }
    }
}
