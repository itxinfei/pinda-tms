package com.itheima.pinda.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

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
     * 向kafka队列发送消息
     */
    public boolean send(String topic, String message){
        try {
            kafkaTemplate.send(topic, message);
        } catch (Exception e) {
            log.error("发送Kafka消息失败: topic={}", topic, e);
            return false;
        }
        return true;
    }
}
