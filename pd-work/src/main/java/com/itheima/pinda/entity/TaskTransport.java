package com.itheima.pinda.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 干线运输任务（两机构间一段，绑定车次/车辆）
 */
@Data
@TableName("work_task_transport")
public class TaskTransport implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 车次 base_transport_trips.id
     */
    private Long tripsId;

    /**
     * 起始机构
     */
    private Long startOrgId;

    /**
     * 目的机构
     */
    private Long endOrgId;

    /**
     * 任务状态：1待执行 2进行中 3待确认 4已完成 5已取消
     */
    private Integer status;

    /**
     * 分配状态：1未分配 2已分配 3待人工分配
     */
    private Integer assignedStatus;

    /**
     * 满载状态：1半载 2满载 3空载
     */
    private Integer loadingStatus;

    /**
     * 车辆 base_truck.id
     */
    private Long truckId;

    /**
     * 提货凭证
     */
    private String pickupPicture;

    /**
     * 货物照片
     */
    private String cargoPicture;

    /**
     * 回单凭证
     */
    private String certificatePicture;

    /**
     * 交付照片
     */
    private String deliverPicture;

    /**
     * 提货经度
     */
    private BigDecimal pickupLongitude;

    /**
     * 提货纬度
     */
    private BigDecimal pickupLatitude;

    /**
     * 交付经度
     */
    private BigDecimal deliverLongitude;

    /**
     * 交付纬度
     */
    private BigDecimal deliverLatitude;

    /**
     * 计划发车时间
     */
    private LocalDateTime planDepartureTime;

    /**
     * 实际发车时间
     */
    private LocalDateTime actualDepartureTime;

    /**
     * 计划到达时间
     */
    private LocalDateTime planArrivalTime;

    /**
     * 实际到达时间
     */
    private LocalDateTime actualArrivalTime;

    /**
     * 计划提货时间
     */
    private LocalDateTime planPickUpTime;

    /**
     * 实际提货时间
     */
    private LocalDateTime actualPickUpTime;

    /**
     * 计划交付时间
     */
    private LocalDateTime planDeliveryTime;

    /**
     * 实际交付时间
     */
    private LocalDateTime actualDeliveryTime;

    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;

    /**
     * 逻辑删除：0 未删 1 已删
     */
    @TableLogic
    private Integer deleted;
}
