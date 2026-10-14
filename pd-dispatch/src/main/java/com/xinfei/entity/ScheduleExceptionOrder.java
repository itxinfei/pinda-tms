package com.xinfei.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 异常调度订单登记实体（disp_exception_order）
 *
 * <p>记录调度过程中无法完成线路规划（ERROR 分组，如起始/目的机构信息缺失）的订单，
 * 供运营人员查询并人工处理。订单ID落库为 BIGINT，pd-oms 侧以 String 协议交互。
 * 主键雪花 Long；tenant_id 由 TenantLineInnerInterceptor 自动填充。</p>
 */
@Data
@TableName("disp_exception_order")
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
     * 主键（雪花）
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 订单ID（oms_order.id，落库 BIGINT）
     */
    private Long orderId;

    /**
     * 发生时所在机构ID（调度发生时所在网点）
     */
    private Long orgId;

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
