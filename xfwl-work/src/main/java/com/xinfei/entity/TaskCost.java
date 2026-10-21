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
 * 趟次成本 —— P0-6d 利润核算前置（《20》OPS-4 §6.2）
 *
 * <p>各类成本按趟归集、人工录入（不引 IoT）；成本口径见《20》§5.7。
 * FIN-4 利润 = Σ(趟关联订单 Order.amount) − Σ(work_task_cost)，利润本身不建表。</p>
 */
@Data
@TableName("work_task_cost")
public class TaskCost implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String TYPE_FUEL = "FUEL";
    public static final String TYPE_TOLL = "TOLL";
    public static final String TYPE_REPAIR = "REPAIR";
    public static final String TYPE_FINE = "FINE";
    public static final String TYPE_LABOR = "LABOR";
    public static final String TYPE_OTHER = "OTHER";

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 运输任务id(趟次, work_task_transport.id) */
    private Long taskTransportId;

    /** 车辆id */
    private Long truckId;

    /** 司机id */
    private Long driverId;

    /** 成本类型: FUEL/TOLL/REPAIR/FINE/LABOR/OTHER */
    private String costType;

    /** 金额 */
    private BigDecimal amount;

    /** 发生时间 */
    private LocalDateTime occurTime;

    /** 凭证图片 */
    private String voucher;

    /** 录入人 */
    private String operator;

    /** 备注 */
    private String remark;

    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
