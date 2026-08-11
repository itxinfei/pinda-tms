package com.itheima.pinda.pay;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 支付渠道抽象接口
 *
 * <p>统一微信支付、支付宝、模拟渠道的接入契约，供 {@link IPayService} 按渠道调用。
 * 各渠道实现负责：预下单（生成拉起支付的参数）、验签（校验支付回调）、查询与退款。</p>
 */
public interface PayChannel {

    /**
     * 渠道编码（与 PaymentOrder.payChannel 一致）
     *
     * @return 渠道编码
     */
    String channelCode();

    /**
     * 创建支付（预下单）
     *
     * @param orderId 订单ID
     * @param payNo   支付流水号
     * @param amount  支付金额
     * @return 预支付参数（JSON，前端拉起支付用）
     */
    String createPayment(String orderId, String payNo, BigDecimal amount);

    /**
     * 校验支付回调签名
     *
     * @param params 回调参数
     * @return 是否合法
     */
    boolean verifyCallback(Map<String, String> params);

    /**
     * 解析回调中的渠道交易号
     *
     * @param params 回调参数
     * @return 渠道交易号
     */
    String parseTradeNo(Map<String, String> params);

    /**
     * 查询支付结果
     *
     * @param orderId 订单ID
     * @param payNo   支付流水号
     * @return true=已支付
     */
    boolean queryPayment(String orderId, String payNo);

    /**
     * 退款
     *
     * @param orderId 订单ID
     * @param payNo   支付流水号
     * @param amount  退款金额
     * @return 是否成功
     */
    boolean refund(String orderId, String payNo, BigDecimal amount);
}
