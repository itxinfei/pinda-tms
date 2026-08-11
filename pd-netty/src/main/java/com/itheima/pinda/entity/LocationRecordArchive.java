package com.itheima.pinda.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * GPS轨迹归档记录（历史轨迹从 pd_truck_location 归档至 pd_truck_location_archive）
 *
 * <p>字段与 {@link LocationRecord} 一致，另补充归档时间 archiveTime。</p>
 */
@Data
@TableName("pd_truck_location_archive")
public class LocationRecordArchive implements Serializable {

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
     */
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
     * 原始入库时间
     */
    private LocalDateTime createTime;

    /**
     * 归档时间
     */
    private LocalDateTime archiveTime;
}
