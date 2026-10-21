package com.xinfei.entity.carrier;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 外协车辆（挂靠承运商，DSP-2 第一期）
 */
@Data
@TableName("base_carrier_truck")
public class XfCarrierTruck {

    /** 状态：正常（可入运力池） */
    public static final int STATUS_NORMAL = 1;
    /** 状态：禁用 */
    public static final int STATUS_DISABLED = 0;
    /** 状态：在途 */
    public static final int STATUS_EN_ROUTE = 2;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 承运商ID（→base_carrier.id） */
    private Long carrierId;

    /** 车牌号 */
    private String licensePlate;

    /** 车辆类型ID（→base_truck_type.id，选填清空需落 null） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long truckTypeId;

    /** 准载重量(kg)（选填，清空需落 null） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal allowableLoad;

    /** 准载体积(m³)（选填，清空需落 null） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal allowableVolume;

    /** 状态 1正常 0禁用 2在途 */
    private Integer status;

    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;

    /** 逻辑删除：0 未删 1 已删 */
    @TableLogic
    private Integer deleted;
}
