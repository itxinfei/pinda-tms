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
 * 司机趟次结算单 —— FIN-2（《21》§FIN-2 第一期定稿 2026-10-20）
 *
 * <p>归集 work_driver_job.status=4 的已完成趟次按司机+账期生成工资单；
 * 状态 0待审 1已审 2已发放（《07》§11.3）：audit 0→1、pay 1→2 条件 UPDATE，
 * 发放=线下打款后内部记账留痕，不接支付通道（D-27）。</p>
 */
@Data
@TableName("work_driver_settlement")
public class DriverSettlement implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 待审 */
    public static final int STATUS_PENDING = 0;
    /** 已审 */
    public static final int STATUS_AUDITED = 1;
    /** 已发放 */
    public static final int STATUS_PAID = 2;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 结算单号 DJS+yyyyMMdd+当日序号 */
    private String settleNo;

    /** 司机id（与 work_driver_job.driver_id 同源） */
    private Long driverId;

    /** 账期起 */
    private LocalDate periodStart;

    /** 账期止 */
    private LocalDate periodEnd;

    /** 完成趟次数 */
    private Integer tripCount;

    /** 底薪（生成时手工项） */
    private BigDecimal baseWage;

    /** 趟次费合计（=Σ明细 amount） */
    private BigDecimal tripFee;

    /** 补贴（油补/过路/餐补，手工项） */
    private BigDecimal allowance;

    /** 扣款（违章/货损/迟到，手工项） */
    private BigDecimal deduction;

    /** 应发=底薪+趟次费+补贴-扣款 */
    private BigDecimal totalAmount;

    /** 状态: 0待审 1已审 2已发放 */
    private Integer status;

    /** 审核备注 */
    private String auditRemark;

    /** 发放时间（pay 时写） */
    private LocalDateTime paidTime;

    /** 备注 */
    private String remark;

    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;

    /** 逻辑删除：0 未删 1 已删 */
    @TableLogic
    private Integer deleted;
}
