package com.xinfei.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 电子发票 —— P0-6c 开票模块
 *
 * <p>按账单申请开票，管理 申请→开具→邮寄→作废 生命周期；**不接真实税控**，
 * invoice_no 测试环境自编（TEST-+时间戳），生产待 P0-7c 数电票回填。</p>
 */
@Data
@TableName("oms_invoice")
public class Invoice implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 状态：待申请（apply 落地态） */
    public static final int STATUS_APPLIED = 1;
    /** 状态：已申请 */
    public static final int STATUS_REQUESTED = 2;
    /** 状态：已开具（issue，自编发票号） */
    public static final int STATUS_ISSUED = 3;
    /** 状态：已邮寄（mail） */
    public static final int STATUS_MAILED = 4;
    /** 状态：已作废（void，必填原因） */
    public static final int STATUS_VOIDED = 5;

    /** 票种：增值税普票 */
    public static final int TYPE_GENERAL = 1;
    /** 票种：增值税专票 */
    public static final int TYPE_SPECIAL = 2;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 发票号（issue 时自编 TEST-+ts；生产待 P0-7c） */
    private String invoiceNo;

    /** 关联账单 id（oms_freight_bill.id） */
    private Long billId;

    /** 客户id（对齐 settle_object_id，散客=GUEST） */
    private String customerId;

    /** 开票金额（=账单应收总额） */
    private BigDecimal amount;

    /** 票种：1普票 2专票 */
    private Integer type;

    /** 状态：1待申请 2已申请 3已开具 4已邮寄 5已作废 */
    private Integer status;

    /** 客户税号 */
    private String taxNo;

    /** 邮寄地址 */
    private String mailAddress;

    /** 开具时间 */
    private LocalDateTime invoiceTime;

    /** 作废原因（status=5 必填） */
    private String voidReason;

    /** 备注 */
    private String remark;

    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
