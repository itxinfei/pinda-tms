package com.itheima.pinda.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 事件发布Feign接口
 *
 * 用于跨模块发布领域事件
 * 避免在pd-web-customer/pd-web-courier中直接依赖pd-dispatch的事件类
 *
 * @author Claude Code
 * @since 2026-07-01
 */
@FeignClient(name = "pd-dispatch", path = "/event")
public interface IEventPublishService {

    /**
     * 发布订单确认事件
     *
     * @param orderId 订单ID
     * @param memberId 会员ID
     * @param amount 订单金额
     * @return 是否发布成功
     */
    @PostMapping("/order-confirmed")
    boolean publishOrderConfirmedEvent(@RequestBody EventMessage event);

    /**
     * 发布揽收完成事件
     *
     * @param orderId 订单ID
     * @param transportOrderId 运单ID
     * @param courierId 快递员ID
     * @return 是否发布成功
     */
    @PostMapping("/pickup-completed")
    boolean publishPickupCompletedEvent(@RequestBody EventMessage event);

    /**
     * 发布订单交付事件
     *
     * @param orderId 订单ID
     * @param transportOrderId 运单ID
     * @param signed 是否签收
     * @param signRemark 签收备注
     * @return 是否发布成功
     */
    @PostMapping("/order-delivered")
    boolean publishOrderDeliveredEvent(@RequestBody EventMessage event);
}
