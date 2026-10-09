package com.itheima.pinda.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 结算单
 *
 * <p>订单签收后自动生成，记录结算对象、应收/应付方向、金额与对账状态，
 * 是财务结算域最小闭环的核心单据。最小闭环阶段一单一单，
 * 后续可按客户+账期归集生成月结账单（pd_freight_bill）。</p>
 */
@Data
@TableName("pd_settlement_order")
public class SettlementOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 结算对象类型：客户
     */
    public static final int OBJECT_MEMBER = 1;

    /**
     * 结算对象类型：司机
     */
    public static final int OBJECT_DRIVER = 2;

    /**
     * 结算对象类型：承运商（外协/加盟）
     */
    public static final int OBJECT_CARRIER = 3;

    /**
     * 方向：应收
     */
    public static final int DIRECTION_RECEIVABLE = 1;

    /**
     * 方向：应付
     */
    public static final int DIRECTION_PAYABLE = 2;

    /**
     * 状态：待对账
     */
    public static final int STATUS_PENDING = 0;

    /**
     * 状态：已对账
     */
    public static final int STATUS_RECONCILED = 1;

    /**
     * 状态：已结算
     */
    public static final int STATUS_SETTLED = 2;

    /**
     * 结算对象为散客时的兜底标识
     */
    public static final String GUEST_OBJECT_ID = "GUEST";

    /**
     * id
     */
    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    /**
     * 结算单号
     */
    private String settlementNo;

    /**
     * 订单id
     */
    private String orderId;

    /**
     * 运单id
     */
    private String transportOrderId;

    /**
     * 结算对象类型：1-客户 2-司机 3-承运商
     */
    private Integer settleObjectType;

    /**
     * 结算对象id
     */
    private String settleObjectId;

    /**
     * 方向：1-应收 2-应付
     */
    private Integer direction;

    /**
     * 周期开始日期
     */
    private LocalDate periodStart;

    /**
     * 周期结束日期
     */
    private LocalDate periodEnd;

    /**
     * 关联订单数
     */
    private Integer orderCount;

    /**
     * 应收金额
     */
    private BigDecimal receivableAmount;

    /**
     * 应付金额
     */
    private BigDecimal payableAmount;

    /**
     * 已结算金额
     */
    private BigDecimal settledAmount;

    /**
     * 状态：0-待对账 1-已对账 2-已结算
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
