package com.xinfei.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * GPS轨迹明细记录（持久化到 iot_truck_location 表）
 *
 * <p>由 {@code RabbitGpsTraceConsumer} 将上报协议模型 {@link LocationEntity}
 * 在落库边界转换而来。类型字段（business_type/coord_system）用数字枚举，
 * 坐标用 DECIMAL，上报时间用 DATETIME。tenant_id 不声明，由多租户插件在 insert 时补入。</p>
 */
@Data
@TableName("iot_truck_location")
public class LocationRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键（雪花，由 MyBatis-Plus 生成）
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
     * 坐标系: 1WGS84 2GCJ02 3BD09 4CGCS2000；落库前坐标已统一为 BD09，故恒为 3
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
     * 入库时间
     */
    private LocalDateTime createTime;
}
