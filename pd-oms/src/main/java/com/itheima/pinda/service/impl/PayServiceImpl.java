package com.itheima.pinda.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.pinda.common.utils.CustomIdGenerator;
import com.itheima.pinda.entity.Order;
import com.itheima.pinda.entity.PaymentOrder;
import com.itheima.pinda.enums.OrderPaymentStatus;
import com.itheima.pinda.pay.PayChannel;
import com.itheima.pinda.service.IOrderService;
import com.itheima.pinda.service.IPayService;
import com.itheima.pinda.service.IPaymentOrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统一支付服务实现
 *
 * <p>创建支付单→按配置选择渠道预下单→回调验签→联动订单支付状态，
 * 支持微信/支付宝/模拟渠道，默认模拟渠道便于开发联调。</p>
 */
@Slf4j
@Service
public class PayServiceImpl implements IPayService {

    @Autowired
    private IPaymentOrderService paymentOrderService;

    @Autowired
    private IOrderService orderService;

    @Autowired
    private CustomIdGenerator idGenerator;

    /**
     * 渠道实现集合（Spring 按类型注入所有 PayChannel 实现）
     */
    @Autowired
    private List<PayChannel> payChannels;

    /**
     * 默认支付渠道（可通过配置 pay.channel 覆盖: wechat/alipay/mock）
     */
    @Value("${pay.channel:mock}")
    private String defaultChannel;

    /**
     * 创建支付（生成支付单并预下单）
     *
     * @param orderId 订单ID
     * @return 支付单（含 prepayParams）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentOrder createPayment(String orderId) {
        Order order = orderService.getById(orderId);
        if (order == null) {
            log.warn("[支付] 订单不存在，无法创建支付: orderId={}", orderId);
            return null;
        }
        // 已支付订单不允许重复支付
        if (OrderPaymentStatus.PAID.getStatus().equals(order.getPaymentStatus())) {
            log.warn("[支付] 订单已支付，拒绝重复创建: orderId={}", orderId);
            return null;
        }
        // 已存在待支付支付单则复用
        LambdaQueryWrapper<PaymentOrder> existsWrapper = new LambdaQueryWrapper<>();
        existsWrapper.eq(PaymentOrder::getOrderId, orderId)
            .eq(PaymentOrder::getStatus, PaymentOrder.STATUS_PENDING);
        PaymentOrder existing = paymentOrderService.getOne(existsWrapper);
        if (existing != null) {
            return existing;
        }

        PayChannel channel = resolveChannel(defaultChannel);
        if (channel == null) {
            log.error("[支付] 未知支付渠道，无法创建支付单: channel={}", defaultChannel);
            return null;
        }
        String payNo = idGenerator.nextId(new PaymentOrder()).toString();

        PaymentOrder paymentOrder = new PaymentOrder();
        paymentOrder.setId(idGenerator.nextId(new PaymentOrder()).toString());
        paymentOrder.setOrderId(orderId);
        paymentOrder.setPayNo(payNo);
        paymentOrder.setPayChannel(channel.channelCode());
        paymentOrder.setAmount(order.getAmount());
        paymentOrder.setStatus(PaymentOrder.STATUS_PENDING);
        paymentOrder.setCreateTime(LocalDateTime.now());
        paymentOrder.setUpdateTime(LocalDateTime.now());

        try {
            String prepayParams = channel.createPayment(orderId, payNo, order.getAmount());
            paymentOrder.setPrepayParams(prepayParams);
        } catch (Exception e) {
            log.error("[支付] 渠道预下单失败: orderId={}, channel={}", orderId, channel.channelCode(), e);
            return null;
        }
        try {
            paymentOrderService.save(paymentOrder);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // 并发创建同一订单支付单时，唯一索引(uk_order_id)兜底：复用已创建的支付单
            log.warn("[支付] 订单[{}]并发创建支付单被唯一索引拦截，复用已有支付单", orderId);
            LambdaQueryWrapper<PaymentOrder> reuseWrapper = new LambdaQueryWrapper<>();
            reuseWrapper.eq(PaymentOrder::getOrderId, orderId)
                .eq(PaymentOrder::getStatus, PaymentOrder.STATUS_PENDING);
            PaymentOrder reused = paymentOrderService.getOne(reuseWrapper);
            return reused != null ? reused : paymentOrder;
        }
        // 模拟渠道：服务端直接完成支付，保持"支付即完成"语义（真实渠道由回调驱动）
        if (PaymentOrder.CHANNEL_MOCK.equals(paymentOrder.getPayChannel())) {
            Map<String, String> mockCallback = new HashMap<>();
            mockCallback.put("payNo", paymentOrder.getPayNo());
            mockCallback.put("tradeNo", "MOCK" + System.currentTimeMillis());
            mockCallback.put("amount", paymentOrder.getAmount() == null ? null : paymentOrder.getAmount().toPlainString());
            boolean paid = handleCallback(PaymentOrder.CHANNEL_MOCK, mockCallback);
            if (paid) {
                PaymentOrder paidOrder = queryPayment(orderId);
                if (paidOrder != null) {
                    return paidOrder;
                }
            }
        }
        log.info("[支付] 创建支付单成功: orderId={}, payNo={}, channel={}, amount={}",
            orderId, payNo, channel.channelCode(), order.getAmount());
        return paymentOrder;
    }

    /**
     * 处理支付回调（验签通过后置支付单为已支付，并联动更新订单支付状态）
     *
     * @param channel 渠道编码
     * @param params  回调参数
     * @return 是否处理成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handleCallback(String channelCode, Map<String, String> params) {
        PayChannel channel = resolveChannel(channelCode);
        if (channel == null) {
            log.warn("[支付] 未知支付渠道: {}", channelCode);
            return false;
        }
        // 验签
        if (!channel.verifyCallback(params)) {
            log.warn("[支付] 回调验签失败: channel={}", channelCode);
            return false;
        }
        String payNo = channel.parseTradeNo(params);
        if (StringUtils.isBlank(payNo)) {
            log.warn("[支付] 回调缺少支付流水号: channel={}", channelCode);
            return false;
        }
        LambdaQueryWrapper<PaymentOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentOrder::getPayNo, payNo);
        PaymentOrder paymentOrder = paymentOrderService.getOne(wrapper);
        if (paymentOrder == null) {
            log.warn("[支付] 回调对应支付单不存在: payNo={}", payNo);
            return false;
        }
        // 渠道一致性校验：回调渠道必须与支付单创建渠道一致，防止跨渠道伪造
        if (!paymentOrder.getPayChannel().equals(channelCode)) {
            log.warn("[支付] 回调渠道与支付单不一致: payNo={}, 支付单渠道={}, 回调渠道={}",
                payNo, paymentOrder.getPayChannel(), channelCode);
            return false;
        }
        // 金额一致性校验：回调金额与支付单金额一致才确认支付，防止篡改金额
        if (params.get("amount") != null) {
            try {
                BigDecimal callbackAmount = new BigDecimal(params.get("amount"));
                if (paymentOrder.getAmount() != null
                        && callbackAmount.compareTo(paymentOrder.getAmount()) != 0) {
                    log.warn("[支付] 回调金额与支付单不一致: payNo={}, 支付单金额={}, 回调金额={}",
                        payNo, paymentOrder.getAmount(), callbackAmount);
                    return false;
                }
            } catch (NumberFormatException e) {
                log.warn("[支付] 回调金额格式非法: payNo={}, amount={}", payNo, params.get("amount"));
                return false;
            }
        }
        if (PaymentOrder.STATUS_PAID == paymentOrder.getStatus()) {
            log.info("[支付] 支付单已处理，跳过重复回调: payNo={}", payNo);
            return true;
        }

        // 置支付单为已支付
        PaymentOrder update = new PaymentOrder();
        update.setId(paymentOrder.getId());
        update.setStatus(PaymentOrder.STATUS_PAID);
        update.setChannelTradeNo(params.get("tradeNo"));
        update.setPayTime(LocalDateTime.now());
        update.setUpdateTime(LocalDateTime.now());
        paymentOrderService.updateById(update);

        // 联动订单支付状态
        Order orderUpdate = new Order();
        orderUpdate.setId(paymentOrder.getOrderId());
        orderUpdate.setPaymentStatus(OrderPaymentStatus.PAID.getStatus());
        orderService.updateById(orderUpdate);

        log.info("[支付] 支付回调处理成功: payNo={}, orderId={}", payNo, paymentOrder.getOrderId());
        return true;
    }

    /**
     * 查询支付状态
     *
     * @param orderId 订单ID
     * @return 支付单
     */
    @Override
    public PaymentOrder queryPayment(String orderId) {
        LambdaQueryWrapper<PaymentOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentOrder::getOrderId, orderId);
        return paymentOrderService.getOne(wrapper);
    }

    /**
     * 退款
     *
     * @param orderId 订单ID
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean refund(String orderId) {
        PaymentOrder paymentOrder = queryPayment(orderId);
        if (paymentOrder == null) {
            log.warn("[支付] 退款失败：无支付单: orderId={}", orderId);
            return false;
        }
        if (PaymentOrder.STATUS_PAID != paymentOrder.getStatus()) {
            log.warn("[支付] 退款失败：支付单未支付或已处理: orderId={}, status={}", orderId, paymentOrder.getStatus());
            return false;
        }
        PayChannel channel = resolveChannel(paymentOrder.getPayChannel());
        boolean success = channel != null && channel.refund(orderId, paymentOrder.getPayNo(), paymentOrder.getAmount());
        if (!success) {
            log.error("[支付] 渠道退款失败: orderId={}, channel={}", orderId, paymentOrder.getPayChannel());
            return false;
        }
        PaymentOrder update = new PaymentOrder();
        update.setId(paymentOrder.getId());
        update.setStatus(PaymentOrder.STATUS_REFUNDED);
        update.setUpdateTime(LocalDateTime.now());
        paymentOrderService.updateById(update);

        // 联动订单支付状态为已退款，保持订单与支付单生命周期一致
        Order orderUpdate = new Order();
        orderUpdate.setId(paymentOrder.getOrderId());
        orderUpdate.setPaymentStatus(OrderPaymentStatus.REFUNDED.getStatus());
        orderService.updateById(orderUpdate);

        log.info("[支付] 退款成功: orderId={}, payNo={}", orderId, paymentOrder.getPayNo());
        return true;
    }

    /**
     * 按渠道编码解析渠道实现
     *
     * @param channelCode 渠道编码
     * @return 渠道实现；未知返回 null
     */
    private PayChannel resolveChannel(String channelCode) {
        if (payChannels != null) {
            for (PayChannel channel : payChannels) {
                if (channel.channelCode().equals(channelCode)) {
                    return channel;
                }
            }
        }
        return null;
    }
}
