package com.itheima.pinda.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * GPS轨迹归档记录（持久化到 iot_truck_location_archive 表）
 *
 * <p>字段与 {@link LocationRecord} 一致，另补充归档时间 archiveTime。
 * tenant_id 由多租户插件在归档 insert 时补入。</p>
 */
@Data
@TableName("iot_truck_location_archive")
public class LocationRecordArchive implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键（沿用主表雪花 id）
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 业务id: 车辆id 或 快递员id
     */
    private Long businessId;

    /**
     * 业务类型: 1车辆 2快递员
     */
    private Integer businessType;

    /**
     * 司机/快递员名称
     */
    private String name;

    /**
     * 司机/快递员电话
     */
    private String phone;

    /**
     * 车牌号
     */
    private String licensePlate;

    /**
     * 经度（BD09）
     */
    private BigDecimal longitude;

    /**
     * 纬度（BD09）
     */
    private BigDecimal latitude;

    /**
     *坐标系: 1WGS84 2GCJ02 3BD09 4CGCS2000
     */
    private Integer coordSystem;

    /**
     * 数据来源: MOBILE/PDA/JT808/NETTY_TCP/HTTP
     */
    private String source;

    /**
     * 所属车队
     */
    private String team;

    /**
     * 运输任务id
     */
    private Long transportTaskId;

    /**
     * 设备上报时间
     */
    @TableField("report_time")
    private LocalDateTime reportTime;

    /**
     * 原始入库时间
     */
    private LocalDateTime createTime;

    /**
     * 归档时间
     */
    private LocalDateTime archiveTime;
}
