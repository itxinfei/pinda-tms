package com.xinfei.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单异常记录（ORD-04）
 *
 * <p>不新增订单状态值（守《07》紧凑 1~12 口径），异常以独立表标记+留痕；
 * 订单本体状态仍由 OrderStatusTransitions 状态机管控。</p>
 */
@Data
@TableName("oms_order_exception")
@Schema
public class OrderException implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键（雪花）
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 订单ID（oms_order.id）
     */
    private Long orderId;

    /**
     * 异常类型：1滞留 2破损 3退回 4其他
     */
    private Integer exceptionType;

    /**
     * 异常描述
     */
    private String description;

    /**
     * 处理状态：1待处理 2已处理
     */
    private Integer handleStatus;

    /**
     * 处理结果（已处理时填写）
     */
    private String handleResult;

    /**
     * 处理人（账号名）
     */
    private String handlerUser;

    /**
     * 处理时间
     */
    private LocalDateTime handleTime;

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
