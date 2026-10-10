package com.itheima.pinda.entity.transportline;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 车次-车辆-司机 关联
 */
@Data
@TableName("base_trips_truck_driver")
public class PdTransportTripsTruckDriver {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 车次ID */
    private Long tripsId;

    /** 车辆ID */
    private Long truckId;

    /** 司机 base_truck_driver.id */
    private Long driverId;
}
