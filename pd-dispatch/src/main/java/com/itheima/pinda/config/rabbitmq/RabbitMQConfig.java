package com.itheima.pinda.config.rabbitmq;

import com.itheima.pinda.mq.EventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ配置
 *
 * 配置交换机、队列、绑定关系
 * 配置消息转换器
 * 配置监听器容器
 *
 * @author Claude Code
 * @since 2026-07-01
 */
@Slf4j
@Configuration
public class RabbitMQConfig {

    /**
     * 领域事件交换机
     */
    public static final String EXCHANGE_DOMAIN_EVENT = "pinda.domain.event.exchange";

    /**
     * 订单确认队列
     */
    public static final String QUEUE_ORDER_CONFIRMED = "pinda.domain.event.queue.order.confirmed";

    /**
     * 揽收完成队列
     */
    public static final String QUEUE_PICKUP_COMPLETED = "pinda.domain.event.queue.pickup.completed";

    /**
     * 订单交付队列
     */
    public static final String QUEUE_ORDER_DELIVERED = "pinda.domain.event.queue.order.delivered";

    /**
     * 死信队列
     */
    public static final String QUEUE_DEAD_LETTER = "pinda.domain.event.queue.dead.letter";

    /**
     * 交换机
     */
    @Bean
    public TopicExchange domainEventExchange() {
        return new TopicExchange(EXCHANGE_DOMAIN_EVENT, true, false);
    }

    /**
     * 订单确认队列
     */
    @Bean
    public Queue orderConfirmedQueue() {
        return QueueBuilder.durable(QUEUE_ORDER_CONFIRMED)
            .withArgument("x-dead-letter-exchange", "")
            .withArgument("x-dead-letter-routing-key", QUEUE_DEAD_LETTER)
            .build();
    }

    /**
     * 揽收完成队列
     */
    @Bean
    public Queue pickupCompletedQueue() {
        return QueueBuilder.durable(QUEUE_PICKUP_COMPLETED)
            .withArgument("x-dead-letter-exchange", "")
            .withArgument("x-dead-letter-routing-key", QUEUE_DEAD_LETTER)
            .build();
    }

    /**
     * 订单交付队列
     */
    @Bean
    public Queue orderDeliveredQueue() {
        return QueueBuilder.durable(QUEUE_ORDER_DELIVERED)
            .withArgument("x-dead-letter-exchange", "")
            .withArgument("x-dead-letter-routing-key", QUEUE_DEAD_LETTER)
            .build();
    }

    /**
     * 死信队列
     */
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(QUEUE_DEAD_LETTER).build();
    }

    /**
     * 绑定：订单确认队列 -> 交换机
     */
    @Bean
    public Binding orderConfirmedBinding() {
        return BindingBuilder.bind(orderConfirmedQueue())
            .to(domainEventExchange())
            .with("ORDER_CONFIRMED");
    }

    /**
     * 绑定：揽收完成队列 -> 交换机
     */
    @Bean
    public Binding pickupCompletedBinding() {
        return BindingBuilder.bind(pickupCompletedQueue())
            .to(domainEventExchange())
            .with("PICKUP_COMPLETED");
    }

    /**
     * 绑定：订单交付队列 -> 交换机
     */
    @Bean
    public Binding orderDeliveredBinding() {
        return BindingBuilder.bind(orderDeliveredQueue())
            .to(domainEventExchange())
            .with("ORDER_DELIVERED");
    }

    /**
     * 消息转换器
     * 使用JSON格式序列化/反序列化
     */
    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate配置
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }

    /**
     * 监听器容器工厂配置
     * 配置并发数、预取数量等
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter messageConverter,
            RabbitProperties rabbitProperties) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setConcurrentConsumers(rabbitProperties.getListener().getSimple().getConcurrency());
        factory.setMaxConcurrentConsumers(rabbitProperties.getListener().getSimple().getMaxConcurrency());
        factory.setPrefetchCount(rabbitProperties.getListener().getSimple().getPrefetch());
        factory.setDefaultRequeueRejected(false); // 消费失败不重新入队，进入死信队列
        return factory;
    }
}
