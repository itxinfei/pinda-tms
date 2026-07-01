package com.itheima.pinda.entity.state;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 状态流转历史表
 *
 * 记录所有状态变更历史，用于审计和追踪
 *
 * @author Claude Code
 * @since 2026-07-01
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("pd_status_transition_history")
public class StatusTransitionHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    /**
     * 业务类型（1-订单，2-运单，3-运输任务）
     */
    private Integer businessType;

    /**
     * 业务ID（订单ID/运单ID/运输任务ID）
     */
    private String businessId;

    /**
     * 业务编号（订单编号/运单编号/运输任务编号）
     */
    private String businessNo;

    /**
     * 操作类型（1-状态变更，2-取消，3-删除）
     */
    private Integer operationType;

    /**
     * 变更前状态
     */
    private Integer beforeStatus;

    /**
     * 变更后状态
     */
    private Integer afterStatus;

    /**
     * 操作人ID
     */
    private String operatorId;

    /**
     * 操作人名称
     */
    private String operatorName;

    /**
     * 操作人类型（1-客户，2-快递员，3-司机，4-系统，5-管理员）
     */
    private Integer operatorType;

    /**
     * 操作备注
     */
    private String remark;

    /**
     * 操作时间
     */
    private LocalDateTime operateTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
