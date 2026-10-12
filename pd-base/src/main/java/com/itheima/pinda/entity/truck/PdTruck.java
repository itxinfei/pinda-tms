package com.itheima.pinda.entity.truck;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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


    /**
     * 车辆类型ID
     * ALWAYS：编辑时清空（选填）也要落库为 null，配合管理端档案弹窗全量更新；
     * 启停/心跳走 UpdateWrapper/原生 SQL 只改对应列，不受此策略影响
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long truckTypeId;

    /** 所属车队ID（选填，清空需落 null） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long fleetId;

    /** 品牌 */
    private String brand;

    /** 车牌号 */
    private String licensePlate;

    /** GPS设备ID（选填，清空需落 null） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String deviceGpsId;

    /** 准载重量(kg)（选填，清空需落 null） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal allowableLoad;

    /** 准载体积(m³)（选填，清空需落 null） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal allowableVolume;

    /** 车辆行驶证ID（由 /truck/{id}/license 单独维护，编辑车辆不透传，禁止加 ALWAYS） */
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
