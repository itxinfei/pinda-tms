package com.itheima.pinda.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * GPS轨迹明细记录（持久化到 MySQL pd_truck_location 表）
 *
 * <p>由 {@link GpsTraceConsumer} 将 Kafka 中的轨迹数据落库，
 * 字段与上报的 {@link LocationEntity} 一一对应，补充入库时间。</p>
 */
@Data
@TableName("pd_truck_location")
public class LocationRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键（businessId#type#currentTime）
     */
    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    /**
     * 业务id: 快递员id 或 车辆id
     */
    private String businessId;

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
     * 类型: truck-车辆 courier-快递员
     */
    private String type;

    /**
     * 经度
     */
    private String lng;

    /**
     * 纬度
     */
    private String lat;

    /**
     * 设备上报时间 yyyyMMddHHmmss
     * <p>current_time 是 MySQL 保留字，必须带反引号，否则批量 INSERT/ORDER BY 直接语法报错。</p>
     */
    @TableField("`current_time`")
    private String currentTime;

    /**
     * 所属车队
     */
    private String team;

    /**
     * 运输任务id
     */
    private String transportTaskId;

    /**
     * 入库时间
     */
    private LocalDateTime createTime;

    /**
     * 坐标系标识（P0-4 北斗字段最小改造 · D-13）
     * <p>WGS84 / GCJ02 / BD09 / CGCS2000，默认 BD09（与后端 BaiduMapUtils 一致）。</p>
     */
    private String coordSystem;

    /**
     * 数据来源（P0-4 北斗字段最小改造 · D-13）
     * <p>MOBILE / PDA / JT808 / NETTY_TCP / HTTP。</p>
     */
    private String source;
}
