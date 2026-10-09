package com.itheima.pinda.entity.truck;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * <p>
 * 车辆信息表
 * </p>
 *
 * @author itcast
 * @since 2019-12-20
 */
@Data
@TableName("pd_truck")
public class PdTruck implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * id
     */
    @TableId(value = "id", type = IdType.INPUT)
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
     * 在线状态（P0-4 北斗字段最小改造 · D-13）
     * <p>0-离线 1-在线。由 pd-netty GpsTraceConsumer 收到 GPS 上报时置 1，
     * 由 @Scheduled 定时任务（每 60s 扫描）将心跳超时（>5min 无上报）的车辆置 0。</p>
     */
    private Integer onlineStatus;

    /**
     * 最后心跳时间（P0-4 北斗字段最小改造 · D-13）
     * <p>最近一次 GPS 上报时间，用于在线状态判定与设备故障排查。</p>
     */
    private LocalDateTime lastHeartbeatTime;
}
