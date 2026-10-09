package com.itheima.pinda.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.pinda.event.OrderDeliveredEvent;
import com.itheima.pinda.service.SettlementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 订单交付事件结算监听器
 *
 * <p>消费 pd-web-courier 在签收时发布的 ORDER_DELIVERED 事件，
 * 触发运费明细与结算单生成。pd-dispatch 中原有 {@code OrderEventListener#handleOrderDelivered}
 * 的结算调用（settlementService.settle）被注释且结算服务缺失，
 * 因边界约定不改动 pd-dispatch，故在 pd-oms 以独立队列消费同一事件，语义等价。</p>
 *
 * <p>队列/绑定由注解在 pd-oms 启动时经 RabbitAdmin 自动声明，
 * 不依赖额外 yml 配置，也不影响 pd-dispatch 既有队列（消息按队列各存一份）。</p>
 */
@Slf4j
@Component
public class SettlementDeliveredListener {

    /**
     * pd-oms 专属交付事件队列
     */
    public static final String OMS_ORDER_DELIVERED_QUEUE = "pinda.oms.queue.order.delivered";

    /**
     * 领域事件交换机（与 EventPublisher / pd-dispatch RabbitMQConfig 一致）
     */
    public static final String DOMAIN_EVENT_EXCHANGE = "pinda.domain.event.exchange";

    /**
     * 交付事件路由键
     */
    public static final String ROUTING_KEY_ORDER_DELIVERED = "ORDER_DELIVERED";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SettlementService settlementService;

    /**
     * 处理交付事件
     *
     * <p>消息为 JSON 字符串，自行反序列化。处理失败仅记录日志、不抛出：
     * 避免默认容器 requeue 导致消息无限重投；签收同步触发点
     * （OrderServiceImpl.updateById）已兜底，且 settle 支持幂等补偿重放。</p>
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = OMS_ORDER_DELIVERED_QUEUE, durable = "true"),
            exchange = @Exchange(value = DOMAIN_EVENT_EXCHANGE, type = "topic", durable = "true"),
            key = ROUTING_KEY_ORDER_DELIVERED))
    public void onOrderDelivered(String message) {
        log.info("[结算-MQ] 收到订单交付事件: {}", message);
        try {
            OrderDeliveredEvent event = objectMapper.readValue(message, OrderDeliveredEvent.class);
            // 拒收不产生应收结算
            if (!event.isSigned()) {
                log.info("[结算-MQ] 订单[{}]为拒收，不生成结算单", event.getOrderId());
                return;
            }
            if (!event.isNeedSettlement()) {
                log.info("[结算-MQ] 订单[{}]标记无需结算，跳过", event.getOrderId());
                return;
            }
            settlementService.settle(event.getOrderId(), event.getTransportOrderId());
        } catch (Exception e) {
            // 不外抛：防止消息无限重入队；靠幂等的 settle 与同步触发点补偿
            log.error("[结算-MQ] 交付事件处理异常（已记录，可人工/重放补偿）: message=" + message, e);
        }
    }
}
