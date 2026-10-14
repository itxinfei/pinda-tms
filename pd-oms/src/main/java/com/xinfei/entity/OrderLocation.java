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
 * 订单位置信息
 */
@Data
@TableName("oms_order_location")
public class OrderLocation implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;


    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 寄件经度
     */
    private BigDecimal sendLongitude;

    /**
     * 寄件纬度
     */
    private BigDecimal sendLatitude;

    /**
     * 收件经度
     */
    private BigDecimal receiveLongitude;

    /**
     * 收件纬度
     */
    private BigDecimal receiveLatitude;

    /**
     * 起始网点ID（旧 sendAgentId）
     */
    private Long sendOrgId;

    /**
     * 目的网点ID（旧 receiveAgentId）
     */
    private Long receiveOrgId;

    /**
     * 状态：0无效 1有效
     */
    private Integer status;

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
