package com.xinfei.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * GPS/人工告警记录（持久化到 iot_alarm_record 表）
 *
 * <p>alarm_type 为数字枚举，码表见 {@link com.xinfei.enums.AlarmTypeEnum}：
 * 1超速 2长时停留 3偏离路线 4车辆离线，人工上报 10-16。
 * tenant_id 不声明，由多租户插件在 insert 时补入。</p>
 */
@Data
@TableName("iot_alarm_record")
public class AlarmRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键（雪花）
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 告警类型（数字码见 AlarmTypeEnum）
     */
    private Integer alarmType;

    /**
     * 运输任务id
     */
    private Long transportTaskId;

    /**
     * 车辆id
     */
    private Long truckId;

    /**
     * 司机/快递员id
     */
    private Long driverId;

    /**
     * 告警点经度（BD09）
     */
    private BigDecimal longitude;

    /**
     * 告警点纬度（BD09）
     */
    private BigDecimal latitude;

    /**
     * 告警内容
     */
    private String alarmContent;

    /**
     * 告警时间
     */
    private LocalDateTime alarmTime;

    /**
     * 处理状态: 0未处理 1已处理
     */
    private Integer status;

    /**
     * 处理人用户id（取自 Sa-Token loginId，对应 sys_user.id）
     */
    private Long handlerId;

    /**
     * 处理人名称（取自 pd-auth 登录时写入的 token-session name）
     */
    private String handlerName;

    /**
     * 处理时间
     */
    private LocalDateTime handleTime;

    /**
     * 处理说明/处置动作
     */
    private String handleRemark;

    /**
     * 入库时间
     */
    private LocalDateTime createTime;
}
