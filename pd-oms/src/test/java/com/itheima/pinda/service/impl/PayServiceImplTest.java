package com.itheima.pinda.service.impl;

import com.itheima.pinda.common.utils.CustomIdGenerator;
import com.itheima.pinda.entity.Order;
import com.itheima.pinda.entity.PaymentOrder;
import com.itheima.pinda.enums.OrderPaymentStatus;
import com.itheima.pinda.pay.PayChannel;
import com.itheima.pinda.service.IOrderService;
import com.itheima.pinda.service.IPaymentOrderService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 统一支付服务单元测试
 *
 * <p>覆盖支付回调安全校验：跨渠道拒绝、金额不一致拒绝、非法金额拒绝、重复回调幂等，
 * 以及模拟渠道自动支付后订单/支付单状态联动。</p>
 */
public class PayServiceImplTest {

    private PayServiceImpl payService;
    private IPaymentOrderService paymentOrderService;
    private IOrderService orderService;
    private PayChannel mockChannel;

    @Before
    public void setUp() throws Exception {
        payService = new PayServiceImpl();
        paymentOrderService = mock(IPaymentOrderService.class);
        orderService = mock(IOrderService.class);
        CustomIdGenerator idGenerator = mock(CustomIdGenerator.class);
        when(idGenerator.nextId(any())).thenReturn(100L, 101L);

        mockChannel = mock(PayChannel.class);
        when(mockChannel.channelCode()).thenReturn(PaymentOrder.CHANNEL_MOCK);
        when(mockChannel.verifyCallback(any())).thenReturn(true);
        when(mockChannel.parseTradeNo(any())).thenReturn("PAY-001");

        List<PayChannel> channels = new ArrayList<>();
        channels.add(mockChannel);

        ReflectionTestUtils.setField(payService, "paymentOrderService", paymentOrderService);
        ReflectionTestUtils.setField(payService, "orderService", orderService);
        ReflectionTestUtils.setField(payService, "idGenerator", idGenerator);
        ReflectionTestUtils.setField(payService, "payChannels", channels);
        ReflectionTestUtils.setField(payService, "defaultChannel", PaymentOrder.CHANNEL_MOCK);
    }

    private PaymentOrder pendingOrder() {
        PaymentOrder po = new PaymentOrder();
        po.setId("1");
        po.setOrderId("ORDER-1");
        po.setPayNo("PAY-001");
        po.setPayChannel(PaymentOrder.CHANNEL_MOCK);
        po.setAmount(new BigDecimal("23.50"));
        po.setStatus(PaymentOrder.STATUS_PENDING);
        return po;
    }

    @Test
    public void testCallbackChannelMismatchRejected() throws Exception {
        PaymentOrder po = pendingOrder();
        when(paymentOrderService.getOne(any())).thenReturn(po);

        // 回调渠道与支付单渠道不一致 → 拒绝
        Map<String, String> params = new HashMap<>();
        params.put("payNo", "PAY-001");
        params.put("amount", "23.50");
        assertFalse(payService.handleCallback("wechat", params));
    }

    @Test
    public void testCallbackAmountMismatchRejected() throws Exception {
        PaymentOrder po = pendingOrder();
        when(paymentOrderService.getOne(any())).thenReturn(po);

        // 回调金额与支付单金额不一致 → 拒绝
        Map<String, String> params = new HashMap<>();
        params.put("payNo", "PAY-001");
        params.put("amount", "99.00");
        assertFalse(payService.handleCallback(PaymentOrder.CHANNEL_MOCK, params));
    }

    @Test
    public void testCallbackMalformedAmountRejected() throws Exception {
        PaymentOrder po = pendingOrder();
        when(paymentOrderService.getOne(any())).thenReturn(po);

        // 回调金额非数字 → 拒绝
        Map<String, String> params = new HashMap<>();
        params.put("payNo", "PAY-001");
        params.put("amount", "abc");
        assertFalse(payService.handleCallback(PaymentOrder.CHANNEL_MOCK, params));
    }

    @Test
    public void testCallbackDuplicateIsIdempotent() throws Exception {
        PaymentOrder po = pendingOrder();
        po.setStatus(PaymentOrder.STATUS_PAID);
        when(paymentOrderService.getOne(any())).thenReturn(po);

        // 已支付订单重复回调 → 幂等返回 true，不抛异常
        Map<String, String> params = new HashMap<>();
        params.put("payNo", "PAY-001");
        params.put("amount", "23.50");
        assertTrue(payService.handleCallback(PaymentOrder.CHANNEL_MOCK, params));
    }

    @Test
    public void testMockCreatePaymentAutoPays() throws Exception {
        // 模拟渠道创建支付单后服务端直接完成支付：支付单与订单均置为已支付
        Order order = new Order();
        order.setId("ORDER-1");
        order.setAmount(new BigDecimal("23.50"));
        order.setPaymentStatus(OrderPaymentStatus.UNPAID.getStatus());
        when(orderService.getById("ORDER-1")).thenReturn(order);

        PaymentOrder po = pendingOrder();
        // 支付完成后的支付单（模拟渠道自动支付后应处于已支付状态）
        PaymentOrder paidPo = pendingOrder();
        paidPo.setStatus(PaymentOrder.STATUS_PAID);
        // createPayment 先查待支付单(无)，回调时查询到待支付单，随后查询支付后支付单(已支付)
        when(paymentOrderService.getOne(any())).thenReturn(null, po, paidPo);
        when(mockChannel.createPayment(any(), any(), any())).thenReturn("{\"mock\":true}");

        PaymentOrder result = payService.createPayment("ORDER-1");
        assertEquals(Integer.valueOf(PaymentOrder.STATUS_PAID), result.getStatus());
        assertEquals(PaymentOrder.CHANNEL_MOCK, result.getPayChannel());
    }

    @Test
    public void testCreatePaymentUnknownChannelReturnsNull() throws Exception {
        ReflectionTestUtils.setField(payService, "defaultChannel", "unknown-channel");
        Order order = new Order();
        order.setId("ORDER-1");
        order.setAmount(new BigDecimal("23.50"));
        when(orderService.getById("ORDER-1")).thenReturn(order);
        when(paymentOrderService.getOne(any())).thenReturn(null);

        // 未知支付渠道 → 优雅返回 null（而非 NPE）
        org.junit.Assert.assertNull(payService.createPayment("ORDER-1"));
    }
}
