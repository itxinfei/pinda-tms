package com.itheima.pinda.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
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
 * 后续可按客户+账期归集生成月结账单。</p>
 */
@Data
@TableName("oms_settlement_order")
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
     * ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 结算单号（STL+雪花）
     */
    private String settlementNo;

    /**
     * 代表订单ID
     */
    private Long orderId;

    /**
     * 运单ID
     */
    private Long transportOrderId;

    /**
     * 结算对象类型：1客户 2司机 3承运商
     */
    private Integer settleObjectType;

    /**
     * 结算对象ID（散客=GUEST）
     */
    private String settleObjectId;

    /**
     * 方向：1应收 2应付
     */
    private Integer direction;

    /**
     * 账期开始
     */
    private LocalDate periodStart;

    /**
     * 账期结束
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
     * 状态：0待对账 1已对账 2已结算
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

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
