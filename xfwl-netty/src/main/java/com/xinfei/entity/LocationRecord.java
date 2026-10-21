package com.xinfei.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xinfei.common.json.BigDecimalFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * GPS轨迹明细记录（持久化到 iot_truck_location 表）
 *
 * <p>由 {@code RabbitGpsTraceConsumer} 将上报协议模型 {@link LocationEntity}
 * 在落库边界转换而来。类型字段（business_type/coord_system）用数字枚举，
 * 坐标用 DECIMAL，上报时间用 DATETIME。tenant_id 不声明，由多租户插件在 insert 时补入。
 * 坐标存库统一 WGS84（coord_system=1，2026-10-17 拍板）；bd09Lng/bd09Lat 为查询出参瞬态字段。</p>
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
     * 经度（存库 WGS84；查询出参经 fillDisplayCoords 归一恒为 WGS84）
     *
     * <p>坐标需 6 位精度（全局 BigDecimalSerializer 默认 ##0.00 为金额设计，
     * 2 位约 1km 颗粒，坐标不可用）。</p>
     */
    @BigDecimalFormat("##0.000000")
    private BigDecimal longitude;

    /**
     * 纬度（存库 WGS84；查询出参经 fillDisplayCoords 归一恒为 WGS84）
     */
    @BigDecimalFormat("##0.000000")
    private BigDecimal latitude;

    /**
     * 坐标系: 1WGS84 2GCJ02 3BD09 4CGCS2000；2026-10-17 起新数据统一落 1（WGS84），
     * 历史行保留原行标（多为 3=BD09），出参按行标归一
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

    /**
     * BD09 经度（查询出参，按 coordSystem 动态转换，不落库）
     *
     * <p>管理端百度地图统一渲染 BD-09（BDS-01），原始字段保持落库口径不动，
     * 转换在查询出口由 {@code CoordTransform} 完成。</p>
     */
    @TableField(exist = false)
    @BigDecimalFormat("##0.000000")
    private BigDecimal bd09Lng;

    /**
     * BD09 纬度（查询出参，按 coordSystem 动态转换，不落库）
     */
    @TableField(exist = false)
    @BigDecimalFormat("##0.000000")
    private BigDecimal bd09Lat;
}
