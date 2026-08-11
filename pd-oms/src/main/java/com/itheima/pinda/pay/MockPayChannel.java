package com.itheima.pinda.pay;

import com.alibaba.fastjson.JSON;
import com.itheima.pinda.entity.PaymentOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 模拟支付渠道（开发/演示环境）
 *
 * <p>用于无微信/支付宝商户配置时模拟支付成功流程：
 * 预下单直接返回可"支付成功"的模拟参数，回调验签恒真，便于联调完整支付链路。</p>
 */
@Slf4j
@Component
public class MockPayChannel implements PayChannel {

    @Override
    public String channelCode() {
        return PaymentOrder.CHANNEL_MOCK;
    }

    @Override
    public String createPayment(String orderId, String payNo, BigDecimal amount) {
        log.info("[模拟支付] 创建模拟支付单: orderId={}, payNo={}, amount={}", orderId, payNo, amount);
        Map<String, Object> params = new HashMap<>();
        params.put("orderId", orderId);
        params.put("payNo", payNo);
        params.put("amount", amount);
        params.put("channel", channelCode());
        params.put("mockPayUrl", "/mock-pay/confirm?payNo=" + payNo);
        return JSON.toJSONString(params);
    }

    @Override
    public boolean verifyCallback(Map<String, String> params) {
        // 模拟渠道校验：回调必须携带支付流水号与交易号，防止空参数伪造
        return params != null
            && params.get("payNo") != null
            && params.get("tradeNo") != null;
    }

    @Override
    public String parseTradeNo(Map<String, String> params) {
        return params == null ? null : params.get("payNo");
    }

    @Override
    public boolean queryPayment(String orderId, String payNo) {
        // 模拟渠道：查询视为已支付（由回调流程置位，此处返回当前状态由调用方维护）
        return false;
    }

    @Override
    public boolean refund(String orderId, String payNo, BigDecimal amount) {
        log.info("[模拟支付] 模拟退款: orderId={}, payNo={}, amount={}", orderId, payNo, amount);
        return true;
    }
}
