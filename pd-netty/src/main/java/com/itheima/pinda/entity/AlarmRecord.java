package com.itheima.pinda.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * GPS 告警记录（持久化到 MySQL pd_alarm_record 表）
 *
 * <p>由 {@link GpsAlertService} 在超速/长时间停留/偏离路线告警触发时写入，
 * 字段对应告警上下文（运输任务、车辆/司机、BD09 经纬度、告警内容）。</p>
 */
@Data
@TableName("pd_alarm_record")
public class AlarmRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键（MyBatis-Plus 雪花算法生成）
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 告警类型: SPEED_OVER-超速 STAY_TOO_LONG-长时间停留 DEVIATE_ROUTE-偏离路线
     * VEHICLE_OFFLINE-车辆离线（2026-10-10 P0-5 新增）
     */
    private String alarmType;

    /**
     * 运输任务id
     */
    private String transportTaskId;

    /**
     * 车辆id（车辆上报时取 businessId）
     */
    private String truckId;

    /**
     * 司机/快递员id（快递员上报时取 businessId）
     */
    private String driverId;

    /**
     * 经度（BD09）
     */
    private Double longitude;

    /**
     * 纬度（BD09）
     */
    private Double latitude;

    /**
     * 告警内容
     */
    private String alarmContent;

    /**
     * 告警时间（pd-netty 服务端时间）
     */
    private LocalDateTime alarmTime;

    /**
     * 处理状态: 0-未处理 1-已处理
     */
    private Integer status;

    /**
     * 入库时间
     */
    private LocalDateTime createTime;
}
