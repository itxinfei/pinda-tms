package com.itheima.pinda.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;

import javax.annotation.PostConstruct;

/**
 * 用于操作Kafka
 */
@Slf4j
@Component
public class KafkaSender {
    public final static String MSG_TOPIC = "tms_order_location";//kafka队列名称

    @Autowired
    private KafkaTemplate<String,String> kafkaTemplate;

    /**
     * 向kafka队列发送消息，返回异步Future以便调用方感知发送结果
     *
     * @return ListenableFuture 发送结果Future，异常时返回null
     */
    public ListenableFuture<SendResult<String, String>> send(String topic, String message){
        try {
            ListenableFuture<SendResult<String, String>> future = kafkaTemplate.send(topic, message);
            // 注册异步回调，确保发送失败（如broker不可用、消息过大）可被感知并告警，
            // 避免消息静默丢失。此回调不影响调用方自行添加的监听器。
            future.addCallback(
                result -> log.debug("Kafka消息发送成功: topic={}", topic),
                ex -> log.error("Kafka消息发送失败: topic={}, 消息将被丢弃", topic, ex)
            );
            return future;
        } catch (Exception e) {
            log.error("发送Kafka消息失败: topic={}", topic, e);
            return null;
        }
    }
}
