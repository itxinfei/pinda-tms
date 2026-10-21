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
 * 司机结算逐趟明细 —— FIN-2（《21》§FIN-2 第一期定稿 2026-10-20）
 *
 * <p>uk(settlement_id, driver_job_id)：一司机作业单只入一张结算单一次，防重复归集。
 * amount = 每趟固定费率（yml {@code xinfei.settlement.trip-fee}，禁硬编码）。</p>
 */
@Data
@TableName("work_driver_settlement_detail")
public class DriverSettlementDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 结算单id（work_driver_settlement.id） */
    private Long settlementId;

    /** 运输任务id（work_task_transport.id） */
    private Long taskTransportId;

    /** 司机作业单id（work_driver_job.id） */
    private Long driverJobId;

    /** 本趟金额（=每趟固定费率） */
    private BigDecimal amount;

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
