package com.itheima.pinda.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单
 */
@Data
@TableName("oms_order")
@Schema
public class Order implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订单ID（雪花，即业务单号）
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 订单号（展示用，可与id同值）
     */
    private String orderNo;

    /**
     * 订单类型，1同城 2城际
     */
    private Integer orderType;

    /**
     * 取件类型，1网点自寄 2上门取件
     */
    private Integer pickupType;

    /**
     * 订单状态（紧凑码 1~12，见 OrderStatus 枚举与 04_oms.sql 文件头映射）
     */
    private Integer status;

    /**
     * 下单会员ID
     */
    private Long memberId;

    /**
     * 寄件人
     */
    private String senderName;

    /**
     * 寄件人电话
     */
    private String senderPhone;

    /**
     * 寄件省ID
     */
    private Integer senderProvinceId;

    /**
     * 寄件市ID
     */
    private Integer senderCityId;

    /**
     * 寄件区县ID
     */
    private Integer senderCountyId;

    /**
     * 寄件详细地址
     */
    private String senderAddress;

    /**
     * 寄件地址簿ID
     */
    private Long senderAddressId;

    /**
     * 收件人
     */
    private String receiverName;

    /**
     * 收件人电话
     */
    private String receiverPhone;

    /**
     * 收件省ID
     */
    private Integer receiverProvinceId;

    /**
     * 收件市ID
     */
    private Integer receiverCityId;

    /**
     * 收件区县ID
     */
    private Integer receiverCountyId;

    /**
     * 收件详细地址
     */
    private String receiverAddress;

    /**
     * 收件地址簿ID
     */
    private Long receiverAddressId;

    /**
     * 当前所属网点ID
     */
    private Long currentOrgId;

    /**
     * 付款方式，1预付 2到付
     */
    private Integer paymentMethod;

    /**
     * 付款状态，1未付 2已付
     */
    private Integer paymentStatus;

    /**
     * 订单金额
     */
    private BigDecimal amount;

    /**
     * 距离(km)
     */
    private BigDecimal distance;

    /**
     * 预计到达时间
     */
    private LocalDateTime estimatedArrivalTime;

    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;

    /**
     * 逻辑删除：0 未删 1 已删
     */
    @TableLogic
    private Integer deleted;
}
