package com.xinfei.entity;

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
 * 客户运费账单（对账载体）—— P0-6b 应收对账闭环
 *
 * <p>按《21》§1.5：账单是对账载体，按 customer_id + 账期归集
 * {@link SettlementOrder}（应收、status∈{待对账,已对账}）生成；
 * <b>不建明细表</b>——明细即 oms_settlement_order 归集，避免双写。</p>
 *
 * <p>状态枚举（新，建表时已回写《07》§11.3）：0待确认 / 1已确认 / 2已核销。</p>
 */
@Data
@TableName("oms_freight_bill")
public class FreightBill implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 状态：待确认（generate 落地态） */
    public static final int STATUS_PENDING = 0;
    /** 状态：已确认（confirm，结算单联动 0→1） */
    public static final int STATUS_CONFIRMED = 1;
    /** 状态：已核销（write-off，结算单联动 1→2） */
    public static final int STATUS_WRITTEN_OFF = 2;

    /**
     * ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 账单号（FB+雪花）
     */
    private String billNo;

    /**
     * 客户id（对齐 oms_settlement_order.settle_object_id，散客=GUEST）
     */
    private String customerId;

    /**
     * 账期开始
     */
    private LocalDate periodStart;

    /**
     * 账期结束
     */
    private LocalDate periodEnd;

    /**
     * 结算单数（归集的应收结算单条数）
     */
    private Integer orderCount;

    /**
     * 应收总额（=Σ归集结算单 receivable_amount）
     */
    private BigDecimal totalAmount;

    /**
     * 状态：0待确认 1已确认 2已核销
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
