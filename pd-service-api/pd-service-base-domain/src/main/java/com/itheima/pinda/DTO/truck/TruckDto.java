package com.itheima.pinda.DTO.truck;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * TruckDto
 */
@Data
public class TruckDto implements Serializable {
    private static final long serialVersionUID = 4541866216281387846L;
    /**
     * id
     */
    private String id;
    /**
     * 车辆类型id
     */
    private String truckTypeId;
    /**
     * 所属车队id
     */
    private String fleetId;
    /**
     * 品牌
     */
    private String brand;
    /**
     * 车牌号码
     */
    private String licensePlate;
    /**
     * GPS设备id
     */
    private String deviceGpsId;
    /**
     * 准载重量
     */
    private BigDecimal allowableLoad;
    /**
     * 准载体积
     */
    private BigDecimal allowableVolume;
    /**
     * 车辆行驶证信息id
     */
    private String truckLicenseId;
    /**
     * 状态 0：禁用 1：正常
     */
    private Integer status;

    /**
     * 在线状态（P0-4 北斗字段最小改造 · D-13）：0-离线 1-在线
     */
    private Integer onlineStatus;

    /**
     * 最后心跳时间（P0-4 北斗字段最小改造 · D-13）
     */
    private LocalDateTime lastHeartbeatTime;
}