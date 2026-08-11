package com.itheima.pinda.pay;

import com.itheima.pinda.entity.PaymentOrder;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * 支付渠道单元测试
 *
 * <p>验证模拟渠道与各渠道的预下单/回调验签/交易号解析等纯逻辑行为。</p>
 */
public class PayChannelTest {

    @Test
    public void testMockChannelPrepayParams() {
        MockPayChannel channel = new MockPayChannel();
        assertEquals(PaymentOrder.CHANNEL_MOCK, channel.channelCode());
        String params = channel.createPayment("order-1", "pay-1", new BigDecimal("23.50"));
        assertNotNull(params);
        assertTrue(params.contains("order-1"));
        assertTrue(params.contains("pay-1"));
        assertTrue(params.contains("mock"));
    }

    @Test
    public void testMockChannelCallbackVerified() {
        MockPayChannel channel = new MockPayChannel();
        // 模拟渠道回调需同时携带 payNo 与 tradeNo 才验签通过（防空参数伪造）
        java.util.Map<String, String> fullParams = new java.util.HashMap<>();
        fullParams.put("payNo", "pay-1");
        fullParams.put("tradeNo", "trade-1");
        assertTrue(channel.verifyCallback(fullParams));
        assertEquals("pay-1", channel.parseTradeNo(fullParams));
        // 缺少 tradeNo 时拒绝
        assertFalse(channel.verifyCallback(Collections.singletonMap("payNo", "pay-1")));
        // 模拟渠道退款成功
        assertTrue(channel.refund("order-1", "pay-1", new BigDecimal("23.50")));
    }

    @Test
    public void testWechatChannelUnconfiguredFallsBackToMockParams() {
        // 未配置商户参数时应降级为模拟预支付参数，保证流程可联调
        WechatPayChannel channel = new WechatPayChannel();
        String params = channel.createPayment("order-1", "pay-1", new BigDecimal("23.50"));
        assertNotNull(params);
        assertTrue(params.contains("pay-1"));
        // 回调验签：未配置商户密钥时 fail-closed（拒绝回调，防止伪造支付）
        assertFalse(channel.verifyCallback(new java.util.HashMap<String, String>() {{
            put("out_trade_no", "pay-1");
            put("result_code", "SUCCESS");
        }}));
        assertEquals("pay-1", channel.parseTradeNo(new java.util.HashMap<String, String>() {{
            put("out_trade_no", "pay-1");
        }}));
    }

    @Test
    public void testAlipayChannelUnconfiguredFallsBackToMockParams() {
        // 未配置商户参数时应降级为模拟预支付参数
        AlipayPayChannel channel = new AlipayPayChannel();
        String params = channel.createPayment("order-1", "pay-1", new BigDecimal("23.50"));
        assertNotNull(params);
        assertTrue(params.contains("pay-1"));
        // 回调验签：未配置商户参数时 fail-closed（拒绝回调，防止伪造支付）
        assertFalse(channel.verifyCallback(new java.util.HashMap<String, String>() {{
            put("out_trade_no", "pay-1");
            put("trade_status", "TRADE_SUCCESS");
        }}));
        assertEquals("pay-1", channel.parseTradeNo(new java.util.HashMap<String, String>() {{
            put("out_trade_no", "pay-1");
        }}));
    }

    @Test
    public void testWechatCallbackRejectedWhenMissingTradeNo() {
        WechatPayChannel channel = new WechatPayChannel();
        assertFalse(channel.verifyCallback(Collections.emptyMap()));
        assertEquals(null, channel.parseTradeNo(Collections.emptyMap()));
    }
}
