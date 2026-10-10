package com.itheima.pinda.entity.truck;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 车辆类型
 */
@Data
@TableName("base_truck_type")
public class PdTruckType {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 车辆类型名称 */
    private String name;

    /** 准载重量(kg) */
    private BigDecimal allowableLoad;

    /** 准载体积(m³) */
    private BigDecimal allowableVolume;

    /** 长(m) */
    private BigDecimal measureLong;

    /** 宽(m) */
    private BigDecimal measureWidth;

    /** 高(m) */
    private BigDecimal measureHigh;

    /** 1启用 0禁用 */
    private Integer status;

    private Long createBy;
    private java.time.LocalDateTime createTime;
    private Long updateBy;
    private java.time.LocalDateTime updateTime;

    /** 逻辑删除：0 未删 1 已删 */
    @TableLogic
    private Integer deleted;
}
