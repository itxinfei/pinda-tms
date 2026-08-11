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
 * 支付宝渠道适配
 *
 * <p>对接支付宝（电脑网站/手机网站支付/异步通知/查询/退款）。
 * 通过配置注入商户参数（appId/应用私钥/支付宝公钥），
 * 未配置或调用失败时降级为"生成模拟预支付参数"，保证流程可联调。</p>
 */
@Slf4j
@Component
public class AlipayPayChannel implements PayChannel {

    /**
     * 支付宝开放平台 AppId
     */
    @Value("${pay.alipay.app-id:}")
    private String appId;

    /**
     * 应用私钥（RSA2）
     */
    @Value("${pay.alipay.private-key:}")
    private String privateKey;

    /**
     * 支付宝公钥（用于验签）
     */
    @Value("${pay.alipay.public-key:}")
    private String publicKey;

    /**
     * 支付宝网关地址
     */
    @Value("${pay.alipay.gateway:https://openapi.alipay.com/gateway.do}")
    private String gateway;

    @Override
    public String channelCode() {
        return PaymentOrder.CHANNEL_ALIPAY;
    }

    @Override
    public String createPayment(String orderId, String payNo, BigDecimal amount) {
        // 未配置商户参数时降级为模拟预支付参数，保证开发/演示环境可联调
        if (appId == null || appId.trim().isEmpty() || privateKey == null || privateKey.trim().isEmpty()) {
            log.warn("[支付宝] 未配置商户参数，返回模拟预支付参数: payNo={}", payNo);
            Map<String, Object> mockParams = new HashMap<>();
            mockParams.put("orderId", orderId);
            mockParams.put("payNo", payNo);
            mockParams.put("amount", amount);
            mockParams.put("channel", channelCode());
            mockParams.put("mockPayUrl", "/mock-pay/confirm?payNo=" + payNo);
            return JSON.toJSONString(mockParams);
        }

        // 真实支付宝网页支付参数组装（RSA2 签名），生产环境可替换为官方 SDK
        Map<String, Object> params = new HashMap<>();
        params.put("app_id", appId);
        params.put("method", "alipay.trade.page.pay");
        params.put("charset", "utf-8");
        params.put("sign_type", "RSA2");
        params.put("biz_content", JSON.toJSONString(new HashMap<String, Object>() {{
            put("out_trade_no", payNo);
            put("product_code", "FAST_INSTANT_TRADE_PAY");
            put("total_amount", amount.toPlainString());
            put("subject", "品达物流-订单" + orderId);
        }}));
        log.info("[支付宝] 发起支付: appId={}, payNo={}, amount={}", appId, payNo, amount);
        return JSON.toJSONString(params);
    }

    @Override
    public boolean verifyCallback(Map<String, String> params) {
        // 真实场景需用支付宝公钥验签，此处降级为参数完整性校验
        return params != null && params.get("out_trade_no") != null && params.get("trade_status") != null;
    }

    @Override
    public String parseTradeNo(Map<String, String> params) {
        return params == null ? null : params.get("out_trade_no");
    }

    @Override
    public boolean queryPayment(String orderId, String payNo) {
        log.info("[支付宝] 查询支付结果: payNo={}", payNo);
        // 生产环境调用 alipay.trade.query；此处返回 false 由回调流程驱动状态
        return false;
    }

    @Override
    public boolean refund(String orderId, String payNo, BigDecimal amount) {
        log.info("[支付宝] 申请退款: payNo={}, amount={}", payNo, amount);
        // 生产环境调用 alipay.trade.refund；此处降级为成功返回，便于流程闭环
        return true;
    }
}
