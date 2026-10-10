package com.itheima.pinda.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 运单与运输任务关联表（多段中转）
 */
@Data
@TableName("work_transport_order_task")
public class TransportOrderTask implements Serializable {

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
     * 运单ID
     */
    private Long transportOrderId;

    /**
     * 运输任务ID（work_task_transport.id）
     */
    private Long taskId;
}
