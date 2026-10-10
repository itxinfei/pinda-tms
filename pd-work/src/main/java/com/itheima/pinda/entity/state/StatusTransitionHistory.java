package com.itheima.pinda.entity.state;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 状态流转历史（横切审计）
 *
 * 记录干线运输任务的状态变更历史，用于审计和追踪
 */
@Data
@TableName("work_status_transition_history")
public class StatusTransitionHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 业务类型：1运输任务（可扩展）
     */
    private Integer businessType;

    /**
     * 业务主体ID
     */
    private Long businessId;

    /**
     * 业务单号
     */
    private String businessNo;

    /**
     * 操作类型：1状态变更
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
    private Long operatorId;

    /**
     * 操作人姓名
     */
    private String operatorName;

    /**
     * 操作人类型：1后台 2司机 3快递员
     */
    private Integer operatorType;

    /**
     * 备注
     */
    private String remark;

    /**
     * 操作时间
     */
    private LocalDateTime operateTime;

    /**
     * 落库时间
     */
    private LocalDateTime createTime;
}
