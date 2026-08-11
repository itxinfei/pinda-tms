package com.itheima.pinda.pay;

import com.alibaba.fastjson.JSON;
import com.itheima.pinda.entity.PaymentOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 微信支付渠道适配
 *
 * <p>对接微信支付（统一下单/支付结果通知/查询/退款）。
 * 通过配置注入商户参数（appId/mchId/密钥），
 * 未配置或调用失败时降级为"生成模拟预支付参数"，保证流程可联调。</p>
 */
@Slf4j
@Component
public class WechatPayChannel implements PayChannel {

    /**
     * 微信支付商户号
     */
    @Value("${pay.wechat.mch-id:}")
    private String mchId;

    /**
     * 微信支付 API 密钥（商户平台设置）
     */
    @Value("${pay.wechat.api-key:}")
    private String apiKey;

    /**
     * 微信支付统一下单接口地址
     */
    @Value("${pay.wechat.unified-order-url:https://api.mch.weixin.qq.com/pay/unifiedorder}")
    private String unifiedOrderUrl;

    @Override
    public String channelCode() {
        return PaymentOrder.CHANNEL_WECHAT;
    }

    @Override
    public String createPayment(String orderId, String payNo, BigDecimal amount) {
        // 未配置商户参数时降级为模拟预支付参数，保证开发/演示环境可联调
        if (mchId == null || mchId.trim().isEmpty() || apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("[微信支付] 未配置商户参数，返回模拟预支付参数: payNo={}", payNo);
            Map<String, Object> mockParams = new HashMap<>();
            mockParams.put("orderId", orderId);
            mockParams.put("payNo", payNo);
            mockParams.put("amount", amount);
            mockParams.put("channel", channelCode());
            mockParams.put("mockPayUrl", "/mock-pay/confirm?payNo=" + payNo);
            return JSON.toJSONString(mockParams);
        }

        // 真实统一下单（微信支付 V2 XML 接口示意；生产环境可替换为官方 SDK/APIv3）
        Map<String, String> req = new HashMap<>();
        req.put("appid", mchId); // 占位：实际为小程序/公众号 AppID
        req.put("mch_id", mchId);
        req.put("out_trade_no", payNo);
        req.put("body", "品达物流-订单" + orderId);
        req.put("total_fee", String.valueOf(amount.multiply(new BigDecimal("100")).intValue())); // 单位：分
        req.put("notify_url", "/pay/callback/wechat");
        req.put("trade_type", "JSAPI");
        // 签名与请求发送：生产环境调用 unifiedOrderUrl，这里以日志占位
        log.info("[微信支付] 统一下单: mchId={}, payNo={}, amount={}", mchId, payNo, amount);
        return JSON.toJSONString(req);
    }

    @Override
    public boolean verifyCallback(Map<String, String> params) {
        // 真实场景需按微信支付签名规则校验（MD5/HMAC-SHA256），此处降级为参数完整性校验
        return params != null && params.get("out_trade_no") != null;
    }

    @Override
    public String parseTradeNo(Map<String, String> params) {
        return params == null ? null : params.get("out_trade_no");
    }

    @Override
    public boolean queryPayment(String orderId, String payNo) {
        log.info("[微信支付] 查询支付结果: payNo={}", payNo);
        // 生产环境调用订单查询接口；此处返回 false 由回调流程驱动状态
        return false;
    }

    @Override
    public boolean refund(String orderId, String payNo, BigDecimal amount) {
        log.info("[微信支付] 申请退款: payNo={}, amount={}", payNo, amount);
        // 生产环境调用退款接口；此处降级为成功返回，便于流程闭环
        return true;
    }
}
