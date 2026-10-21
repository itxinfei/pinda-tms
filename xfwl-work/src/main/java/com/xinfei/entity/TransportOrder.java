package com.xinfei.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 运单
 */
@Data
@TableName("work_transport_order")
public class TransportOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;


    /**
     * 订单ID（来源订单 oms_order.id）
     */
    private Long orderId;

    /**
     * 运单状态：1新建 2已装车 3到达 4到达终端网点 5已签收 6拒收
     */
    private Integer status;

    /**
     * 调度状态：1待调度 2未匹配线路 3已调度
     */
    private Integer schedulingStatus;

    /**
     * 父运单ID（合单时指向主运单，ORD-05；拆单/未合并时为 null）
     */
    private Long parentId;

    /**
     * 合并状态（ORD-05）：normal 普通 / merged 合并组主单 / split_child 合并组子单
     */
    private String mergeStatus;

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
