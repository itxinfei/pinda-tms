package com.itheima.pinda.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置类
 * 自动声明 GPS 轨迹相关的交换机、队列和绑定
 */
@Configuration
public class RabbitConfig {

    public static final String GPS_EXCHANGE = "pinda.gps.exchange";
    public static final String GPS_QUEUE = "pinda.gps.queue";
    public static final String GPS_ROUTING_KEY = "gps.#";

    /**
     * GPS 轨迹交换机
     */
    @Bean
    public TopicExchange gpsExchange() {
        return ExchangeBuilder.topicExchange(GPS_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * GPS 轨迹队列
     */
    @Bean
    public Queue gpsQueue() {
        return QueueBuilder.durable(GPS_QUEUE)
                .build();
    }

    /**
     * GPS 轨迹队列绑定到交换机
     */
    @Bean
    public Binding gpsBinding(Queue gpsQueue, TopicExchange gpsExchange) {
        return BindingBuilder.bind(gpsQueue)
                .to(gpsExchange)
                .with(GPS_ROUTING_KEY);
    }
}
