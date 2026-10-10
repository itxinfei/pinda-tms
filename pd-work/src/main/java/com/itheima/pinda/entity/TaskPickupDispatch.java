package com.itheima.pinda.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 末端取件/派件任务（一个订单一条）
 */
@Data
@TableName("work_pickup_dispatch_task")
public class TaskPickupDispatch implements Serializable {

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
     * 订单ID（oms_order.id）
     */
    private Long orderId;

    /**
     * 任务类型：1取件 2派件
     */
    private Integer taskType;

    /**
     * 任务状态：1待执行 2进行中 3待确认 4已完成 5已取消
     */
    private Integer status;

    /**
     * 签收状态：1签收 2拒收
     */
    private Integer signStatus;

    /**
     * 所属网点 sys_org.id
     */
    private Long orgId;

    /**
     * 快递员 sys_user.id
     */
    private Long courierId;

    /**
     * 分配状态：1未分配 2已分配 3待人工分配
     */
    private Integer assignedStatus;

    /**
     * 预计开始时间
     */
    private LocalDateTime estimatedStartTime;

    /**
     * 实际开始时间
     */
    private LocalDateTime actualStartTime;

    /**
     * 预计完成时间
     */
    private LocalDateTime estimatedEndTime;

    /**
     * 实际完成时间
     */
    private LocalDateTime actualEndTime;

    /**
     * 确认时间
     */
    private LocalDateTime confirmTime;

    /**
     * 取消时间
     */
    private LocalDateTime cancelTime;

    /**
     * 备注
     */
    private String mark;

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
