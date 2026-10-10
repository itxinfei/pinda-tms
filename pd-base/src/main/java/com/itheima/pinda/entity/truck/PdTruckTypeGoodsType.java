package com.itheima.pinda.entity.truck;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 车辆类型-货物类型 关联
 */
@Data
@TableName("base_truck_type_goods")
public class PdTruckTypeGoodsType {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 车辆类型ID */
    private Long truckTypeId;

    /** 货物类型ID */
    private Long goodsTypeId;
}
