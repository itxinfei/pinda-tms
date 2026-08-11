package com.itheima.pinda.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单支付单实体
 *
 * <p>统一承载微信/支付宝/模拟渠道的支付流水，
 * 记录支付状态与渠道预支付参数，支撑支付流程闭环。</p>
 */
@Data
@TableName("pd_payment_order")
public class PaymentOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 状态：待支付
     */
    public static final int STATUS_PENDING = 0;

    /**
     * 状态：已支付
     */
    public static final int STATUS_PAID = 1;

    /**
     * 状态：已关闭
     */
    public static final int STATUS_CLOSED = 2;

    /**
     * 状态：已退款
     */
    public static final int STATUS_REFUNDED = 3;

    /**
     * 渠道：微信
     */
    public static final String CHANNEL_WECHAT = "wechat";

    /**
     * 渠道：支付宝
     */
    public static final String CHANNEL_ALIPAY = "alipay";

    /**
     * 渠道：模拟（开发/演示环境）
     */
    public static final String CHANNEL_MOCK = "mock";

    /**
     * id
     */
    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    /**
     * 订单ID
     */
    private String orderId;

    /**
     * 支付流水号（系统生成，唯一）
     */
    private String payNo;

    /**
     * 支付渠道: wechat-微信 alipay-支付宝 mock-模拟
     */
    private String payChannel;

    /**
     * 支付金额
     */
    private BigDecimal amount;

    /**
     * 状态: 0-待支付 1-已支付 2-已关闭 3-已退款
     */
    private Integer status;

    /**
     * 渠道预支付参数（JSON，供前端拉起支付）
     */
    private String prepayParams;

    /**
     * 渠道交易号（支付成功后回填）
     */
    private String channelTradeNo;

    /**
     * 支付时间
     */
    private LocalDateTime payTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
