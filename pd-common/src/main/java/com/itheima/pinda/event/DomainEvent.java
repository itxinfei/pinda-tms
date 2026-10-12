package com.itheima.pinda.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 领域事件基类
 *
 * 所有领域事件都继承此类
 * 实现Serializable以支持消息队列传输
 *
 * @author Claude Code
 * @since 2026-07-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class DomainEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 事件ID（全局唯一）
     */
    private String eventId;

    /**
     * 事件类型
     */
    private String eventType;

    /**
     * 事件发生时间
     * 钉死序列化契约：EventPublisher 走各服务容器 ObjectMapper，此前 Boot 默认输出
     * ISO 带 T+纳秒（"2026-10-12T18:51:02.509947852"），与全局 "yyyy-MM-dd HH:mm:ss"
     * 契约冲突，已导致 5 处消费端解码补丁（manager/customer Feign、courier/driver、
     * dispatch MQ 消费）。@JsonFormat 注解优先级高于一切 ObjectMapper 配置，
     * 生产端输出与消费端解析同时锁定，事件负载格式从此确定。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime eventTime;

    /**
     * 事件版本
     */
    private String eventVersion = "1.0";

    public DomainEvent(String eventType) {
        this.eventId = java.util.UUID.randomUUID().toString();
        this.eventType = eventType;
        this.eventTime = LocalDateTime.now();
    }
}
