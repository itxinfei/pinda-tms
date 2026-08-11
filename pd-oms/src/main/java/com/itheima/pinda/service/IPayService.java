package com.itheima.pinda.service;

import com.itheima.pinda.entity.PaymentOrder;

import java.util.Map;

/**
 * 统一支付服务
 *
 * <p>对外提供支付创建、回调处理、查询与退款能力，
 * 内部根据配置选择微信/支付宝/模拟渠道，并维护支付单状态与订单支付状态联动。</p>
 */
public interface IPayService {

    /**
     * 创建支付（生成支付单并预下单）
     *
     * @param orderId 订单ID
     * @return 支付单（含 prepayParams）
     */
    PaymentOrder createPayment(String orderId);

    /**
     * 处理支付回调（验签通过后置支付单为已支付，并联动更新订单支付状态）
     *
     * @param channel 渠道编码
     * @param params  回调参数
     * @return 是否处理成功
     */
    boolean handleCallback(String channel, Map<String, String> params);

    /**
     * 查询支付状态
     *
     * @param orderId 订单ID
     * @return 支付单
     */
    PaymentOrder queryPayment(String orderId);

    /**
     * 退款
     *
     * @param orderId 订单ID
     * @return 是否成功
     */
    boolean refund(String orderId);
}
