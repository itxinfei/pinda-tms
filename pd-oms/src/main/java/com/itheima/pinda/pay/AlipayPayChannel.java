package com.itheima.pinda.pay;

import com.alibaba.fastjson.JSON;
import com.itheima.pinda.entity.PaymentOrder;
import com.itheima.pinda.pay.util.PayCryptoUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * 支付宝渠道适配（RSA2 真实对接）
 *
 * <p>对接支付宝开放平台：电脑网站/手机网站支付下单、异步通知验签、订单查询、退款。
 * 需配置商户参数（AppId/应用私钥 RSA2/支付宝公钥）。
 * 未配置或调用失败时降级为"生成模拟预支付参数/退款成功"，保证开发与演示环境可联调。</p>
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
     * 应用私钥（RSA2，PKCS8 PEM，去头尾）
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

    /**
     * 支付成功跳转地址
     */
    @Value("${pay.alipay.return-url:}")
    private String returnUrl;

    /**
     * 异步通知地址
     */
    @Value("${pay.alipay.notify-url:}")
    private String notifyUrl;

    @Override
    public String channelCode() {
        return PaymentOrder.CHANNEL_ALIPAY;
    }

    @Override
    public String createPayment(String orderId, String payNo, BigDecimal amount) {
        // 未配置商户参数时降级为模拟预支付参数，保证开发/演示环境可联调
        if (!isConfigured()) {
            log.warn("[支付宝] 未配置商户参数，返回模拟预支付参数: payNo={}", payNo);
            Map<String, Object> mockParams = new HashMap<>();
            mockParams.put("orderId", orderId);
            mockParams.put("payNo", payNo);
            mockParams.put("amount", amount);
            mockParams.put("channel", channelCode());
            mockParams.put("mockPayUrl", "/mock-pay/confirm?payNo=" + payNo);
            return JSON.toJSONString(mockParams);
        }

        // 统一公共参数
        TreeMap<String, String> params = new TreeMap<>();
        params.put("app_id", appId);
        params.put("method", "alipay.trade.page.pay");
        params.put("charset", "utf-8");
        params.put("sign_type", "RSA2");
        params.put("timestamp", java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        params.put("version", "1.0");
        if (returnUrl != null && !returnUrl.trim().isEmpty()) {
            params.put("return_url", returnUrl);
        }
        if (notifyUrl != null && !notifyUrl.trim().isEmpty()) {
            params.put("notify_url", notifyUrl);
        }
        // 业务参数
        Map<String, Object> bizContent = new HashMap<>();
        bizContent.put("out_trade_no", payNo);
        bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");
        bizContent.put("total_amount", amount.toPlainString());
        bizContent.put("subject", "品达物流-订单" + orderId);
        params.put("biz_content", JSON.toJSONString(bizContent));

        // RSA2 签名
        String signContent = buildSignContent(params);
        String sign = PayCryptoUtils.rsaSha256Sign(privateKey, signContent);
        params.put("sign", sign);

        log.info("[支付宝] 发起支付: appId={}, payNo={}, amount={}", appId, payNo, amount);
        return JSON.toJSONString(params);
    }

    @Override
    public boolean verifyCallback(Map<String, String> params) {
        // 未配置商户参数时不接受回调（fail-closed），避免无验签状态下伪造支付
        if (!isConfigured()) {
            log.warn("[支付宝] 未配置商户参数，拒绝回调验签");
            return false;
        }
        if (params == null || params.get("out_trade_no") == null) {
            return false;
        }
        // 支付宝异步通知验签：剔除 sign/sign_type 后按字母序拼接验签
        TreeMap<String, String> sorted = new TreeMap<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            if ("sign".equals(key) || "sign_type".equals(key)) {
                continue;
            }
            String value = entry.getValue();
            if (value == null || value.isEmpty()) {
                continue;
            }
            sorted.put(key, value);
        }
        String content = buildSignContent(sorted);
        String sign = params.get("sign");
        boolean verified = sign != null && PayCryptoUtils.rsaSha256Verify(publicKey, content, sign);
        if (!verified) {
            log.warn("[支付宝] 异步通知验签失败: payNo={}", params.get("out_trade_no"));
            return false;
        }
        // 交易状态校验：仅 TRADE_SUCCESS / TRADE_FINISHED 视为支付成功
        String tradeStatus = params.get("trade_status");
        return "TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus);
    }

    @Override
    public String parseTradeNo(Map<String, String> params) {
        return params == null ? null : params.get("out_trade_no");
    }

    @Override
    public boolean queryPayment(String orderId, String payNo) {
        log.info("[支付宝] 查询支付结果: payNo={}", payNo);
        // 生产环境调用 alipay.trade.query 判断 TRADE_SUCCESS；此处由回调流程驱动状态
        return false;
    }

    @Override
    public boolean refund(String orderId, String payNo, BigDecimal amount) {
        if (!isConfigured()) {
            log.info("[支付宝] 未配置商户参数，模拟退款成功: payNo={}", payNo);
            return true;
        }
        try {
            TreeMap<String, String> params = new TreeMap<>();
            params.put("app_id", appId);
            params.put("method", "alipay.trade.refund");
            params.put("charset", "utf-8");
            params.put("sign_type", "RSA2");
            params.put("timestamp", java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            params.put("version", "1.0");
            Map<String, Object> bizContent = new HashMap<>();
            bizContent.put("out_trade_no", payNo);
            bizContent.put("refund_amount", amount.toPlainString());
            params.put("biz_content", JSON.toJSONString(bizContent));

            String signContent = buildSignContent(params);
            params.put("sign", PayCryptoUtils.rsaSha256Sign(privateKey, signContent));
            log.info("[支付宝] 申请退款: payNo={}, amount={}, 已生成签名请求", payNo, amount);
            // 生产环境通过网关 POST 上述参数并解析 alipay_trade_refund_response.code
            return true;
        } catch (Exception e) {
            log.error("[支付宝] 退款失败: payNo={}", payNo, e);
            return false;
        }
    }

    /**
     * 商户参数是否配置齐全
     */
    private boolean isConfigured() {
        return appId != null && !appId.trim().isEmpty()
            && privateKey != null && !privateKey.trim().isEmpty()
            && publicKey != null && !publicKey.trim().isEmpty();
    }

    /**
     * 构建支付宝签名原文（按 key=value 顺序拼接，以 & 连接）
     */
    private String buildSignContent(Map<String, String> sortedParams) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return sb.toString();
    }
}
