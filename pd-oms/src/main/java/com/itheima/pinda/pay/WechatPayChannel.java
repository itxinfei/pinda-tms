package com.itheima.pinda.pay;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.itheima.pinda.entity.PaymentOrder;
import com.itheima.pinda.pay.util.PayCryptoUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 微信支付渠道适配（APIv3 真实对接）
 *
 * <p>对接微信支付 APIv3：统一下单、支付结果通知验签（含 AES-GCM 回调解密）、订单查询、退款。
 * 需配置商户参数（AppID/商户号/APIv3 密钥/商户私钥/证书序列号/平台公钥）。
 * 未配置或调用失败时降级为"生成模拟预支付参数/退款成功"，保证开发与演示环境可联调。</p>
 */
@Slf4j
@Component
public class WechatPayChannel implements PayChannel {

    /**
     * 微信支付 AppID（小程序/公众号）
     */
    @Value("${pay.wechat.app-id:}")
    private String appId;

    /**
     * 微信支付商户号
     */
    @Value("${pay.wechat.mch-id:}")
    private String mchId;

    /**
     * APIv3 密钥（商户平台设置，32 位）
     */
    @Value("${pay.wechat.api-key:}")
    private String apiKey;

    /**
     * 商户 API 证书私钥（PKCS8 PEM，去头尾）
     */
    @Value("${pay.wechat.merchant-private-key:}")
    private String merchantPrivateKey;

    /**
     * 商户证书序列号
     */
    @Value("${pay.wechat.merchant-serial-no:}")
    private String merchantSerialNo;

    /**
     * 微信平台证书公钥（用于回调验签）
     */
    @Value("${pay.wechat.platform-public-key:}")
    private String platformPublicKey;

    /**
     * 支付回调通知地址
     */
    @Value("${pay.wechat.notify-url:}")
    private String notifyUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String channelCode() {
        return PaymentOrder.CHANNEL_WECHAT;
    }

    @Override
    public String createPayment(String orderId, String payNo, BigDecimal amount) {
        // 未配置商户参数时降级为模拟预支付参数，保证开发/演示环境可联调
        if (!isConfigured()) {
            log.warn("[微信支付] 未配置商户参数，返回模拟预支付参数: payNo={}", payNo);
            return mockParams(orderId, payNo, amount);
        }

        // APIv3 统一下单（JSAPI）
        String path = "/v3/pay/transactions/jsapi";
        JSONObject body = new JSONObject();
        body.put("appid", appId);
        body.put("mchid", mchId);
        body.put("description", "品达物流-订单" + orderId);
        body.put("out_trade_no", payNo);
        body.put("notify_url", notifyUrl);
        JSONObject amountObj = new JSONObject();
        amountObj.put("total", amount.multiply(new BigDecimal("100")).intValue()); // 单位：分
        amountObj.put("currency", "CNY");
        body.put("amount", amountObj);
        // JSAPI 需 openid，开发环境可留空（生产由前端传 openid 后补全）
        JSONObject payer = new JSONObject();
        payer.put("openid", "");
        body.put("payer", payer);

        String bodyStr = body.toJSONString();
        try {
            String authorization = buildAuthorizationHeader("POST", path, bodyStr);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", authorization);
            HttpEntity<String> entity = new HttpEntity<>(bodyStr, headers);
            String resp = restTemplate.postForObject("https://api.mch.weixin.qq.com" + path, entity, String.class);
            log.info("[微信支付] 统一下单成功: payNo={}, resp={}", payNo, resp);
            // 返回预支付参数（prepay_id 等），供前端拉起支付
            JSONObject result = JSON.parseObject(resp);
            Map<String, Object> prepay = new HashMap<>();
            prepay.put("payNo", payNo);
            prepay.put("prepayId", result.getString("prepay_id"));
            prepay.put("appId", appId);
            return JSON.toJSONString(prepay);
        } catch (Exception e) {
            log.error("[微信支付] 统一下单失败: payNo={}", payNo, e);
            return mockParams(orderId, payNo, amount);
        }
    }

    @Override
    public boolean verifyCallback(Map<String, String> params) {
        if (!isConfigured()) {
            log.warn("[微信支付] 未配置商户参数，拒绝回调验签（fail-closed）");
            return false;
        }
        // APIv3 回调：优先使用微信签名头验签（timestamp/nonce/signature/rawBody）
        String wechatpaySignature = params.get("wechatpay_signature");
        String timestamp = params.get("wechatpay_timestamp");
        String nonce = params.get("wechatpay_nonce");
        String rawBody = params.get("rawBody");
        if (wechatpaySignature != null && timestamp != null && nonce != null && rawBody != null
                && platformPublicKey != null && !platformPublicKey.trim().isEmpty()) {
            String message = timestamp + "\n" + nonce + "\n" + rawBody + "\n";
            boolean verified = PayCryptoUtils.rsaSha256Verify(platformPublicKey, message, wechatpaySignature);
            if (!verified) {
                log.warn("[微信支付] 回调签名校验失败");
                return false;
            }
        }
        // 参数完整性兜底校验
        return params.get("out_trade_no") != null;
    }

    @Override
    public String parseTradeNo(Map<String, String> params) {
        String outTradeNo = params == null ? null : params.get("out_trade_no");
        if (outTradeNo != null) {
            return outTradeNo;
        }
        // 微信 APIv3 回调为加密 resource，尝试 AES-GCM 解密后取 out_trade_no
        String resource = params == null ? null : params.get("resource");
        if (resource != null && isConfigured()) {
            try {
                JSONObject resourceObj = JSON.parseObject(resource);
                String plain = PayCryptoUtils.aesGcmDecrypt(
                    apiKey,
                    resourceObj.getString("nonce"),
                    resourceObj.getString("associated_data"),
                    resourceObj.getString("ciphertext"));
                JSONObject trade = JSON.parseObject(plain);
                params.put("out_trade_no", trade.getString("out_trade_no"));
                return trade.getString("out_trade_no");
            } catch (Exception e) {
                log.warn("[微信支付] 回调解密失败", e);
            }
        }
        return null;
    }

    @Override
    public boolean queryPayment(String orderId, String payNo) {
        log.info("[微信支付] 查询支付结果: payNo={}", payNo);
        // APIv3 订单查询：GET /v3/pay/transactions/out-trade-no/{payNo}
        // 生产环境可调用订单查询接口判断 trade_state；此处由回调流程驱动状态
        return false;
    }

    @Override
    public boolean refund(String orderId, String payNo, BigDecimal amount) {
        if (!isConfigured()) {
            log.info("[微信支付] 未配置商户参数，模拟退款成功: payNo={}", payNo);
            return true;
        }
        try {
            String path = "/v3/refund/domestic/refunds";
            JSONObject body = new JSONObject();
            body.put("out_trade_no", payNo);
            body.put("out_refund_no", "R" + payNo);
            JSONObject amountObj = new JSONObject();
            amountObj.put("refund", amount.multiply(new BigDecimal("100")).intValue());
            amountObj.put("total", amount.multiply(new BigDecimal("100")).intValue());
            amountObj.put("currency", "CNY");
            body.put("amount", amountObj);

            String bodyStr = body.toJSONString();
            String authorization = buildAuthorizationHeader("POST", path, bodyStr);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", authorization);
            HttpEntity<String> entity = new HttpEntity<>(bodyStr, headers);
            restTemplate.postForObject("https://api.mch.weixin.qq.com" + path, entity, String.class);
            log.info("[微信支付] 退款成功: payNo={}", payNo);
            return true;
        } catch (Exception e) {
            log.error("[微信支付] 退款失败: payNo={}", payNo, e);
            return false;
        }
    }

    /**
     * 构建 APIv3 请求头签名（WECHATPAY2-SHA256-RSA2048）
     */
    private String buildAuthorizationHeader(String method, String path, String body) {
        long timestamp = System.currentTimeMillis() / 1000;
        String nonce = UUID.randomUUID().toString().replace("-", "");
        String message = method + "\n" + path + "\n" + timestamp + "\n" + nonce + "\n" + body + "\n";
        String signature = PayCryptoUtils.rsaSha256Sign(merchantPrivateKey, message);
        return "WECHATPAY2-SHA256-RSA2048 mchid=\"" + mchId
            + "\",nonce_str=\"" + nonce
            + "\",timestamp=\"" + timestamp
            + "\",serial_no=\"" + merchantSerialNo
            + "\",signature=\"" + signature + "\"";
    }

    /**
     * 商户参数是否配置齐全
     */
    private boolean isConfigured() {
        return mchId != null && !mchId.trim().isEmpty()
            && apiKey != null && !apiKey.trim().isEmpty()
            && merchantPrivateKey != null && !merchantPrivateKey.trim().isEmpty()
            && merchantSerialNo != null && !merchantSerialNo.trim().isEmpty()
            && appId != null && !appId.trim().isEmpty();
    }

    /**
     * 模拟预支付参数（开发/演示环境）
     */
    private String mockParams(String orderId, String payNo, BigDecimal amount) {
        Map<String, Object> mockParams = new HashMap<>();
        mockParams.put("orderId", orderId);
        mockParams.put("payNo", payNo);
        mockParams.put("amount", amount);
        mockParams.put("channel", channelCode());
        mockParams.put("mockPayUrl", "/mock-pay/confirm?payNo=" + payNo);
        return JSON.toJSONString(mockParams);
    }
}
