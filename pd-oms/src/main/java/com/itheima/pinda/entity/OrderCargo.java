package com.itheima.pinda.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 货物
 */
@Data
@TableName("oms_order_cargo")
public class OrderCargo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 运单ID（统一旧 tranOrderId 命名）
     */
    private Long transportOrderId;

    /**
     * 货物类型ID
     */
    private Long goodsTypeId;

    /**
     * 货物名称
     */
    private String name;

    /**
     * 货物单位
     */
    private String unit;

    /**
     * 货值（保价）
     */
    private BigDecimal cargoValue;

    /**
     * 货物条码
     */
    private String cargoBarcode;

    /**
     * 数量
     */
    private Integer quantity;

    /**
     * 单件体积(m³)
     */
    private BigDecimal volume;

    /**
     * 单件重量(kg)
     */
    private BigDecimal weight;

    /**
     * 总体积(m³)
     */
    private BigDecimal totalVolume;

    /**
     * 总重量(kg)
     */
    private BigDecimal totalWeight;

    /**
     * 备注
     */
    private String remark;

    private Long createBy;
    private java.time.LocalDateTime createTime;
    private Long updateBy;
    private java.time.LocalDateTime updateTime;

    /**
     * 逻辑删除：0 未删 1 已删
     */
    @TableLogic
    private Integer deleted;
}
