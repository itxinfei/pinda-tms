package com.itheima.pinda.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 异常调度订单登记实体
 *
 * <p>记录调度过程中无法完成线路规划（ERROR 分组，如起始/目的机构信息缺失）的订单，
 * 供运营人员查询并人工处理。</p>
 */
@Data
@TableName("pd_schedule_exception_order")
public class ScheduleExceptionOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 状态：待处理
     */
    public static final int STATUS_PENDING = 0;

    /**
     * 状态：已处理
     */
    public static final int STATUS_HANDLED = 1;

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
     * 当前机构ID（调度发生时所在网点）
     */
    private String agencyId;

    /**
     * 异常原因（如：起始/目的机构信息缺失）
     */
    private String reason;

    /**
     * 状态：0-待处理 1-已处理
     */
    private Integer status;

    /**
     * 处理备注
     */
    private String remark;

    /**
     * 登记时间
     */
    private LocalDateTime createTime;

    /**
     * 处理时间
     */
    private LocalDateTime handleTime;
}
