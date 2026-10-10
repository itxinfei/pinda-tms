package com.itheima.pinda.entity.truck;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 车辆
 */
@Data
@TableName("base_truck")
public class PdTruck {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 车辆类型ID */
    private Long truckTypeId;

    /** 所属车队ID */
    private Long fleetId;

    /** 品牌 */
    private String brand;

    /** 车牌号 */
    private String licensePlate;

    /** GPS设备ID */
    private String deviceGpsId;

    /** 准载重量(kg) */
    private BigDecimal allowableLoad;

    /** 准载体积(m³) */
    private BigDecimal allowableVolume;

    /** 车辆行驶证ID */
    private Long truckLicenseId;

    /** 1正常 0禁用 */
    private Integer status;

    /** 在线状态 1在线 0离线（pd-netty 心跳维护，VEHICLE_OFFLINE 告警依赖） */
    private Integer onlineStatus;

    /** 最后心跳时间 */
    private LocalDateTime lastHeartbeatTime;

    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;

    /** 逻辑删除：0 未删 1 已删 */
    @TableLogic
    private Integer deleted;
}
