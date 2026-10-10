package com.itheima.pinda.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 司机作业单（司机视角，改派产生新单 status=3）
 */
@Data
@TableName("work_driver_job")
public class DriverJob implements Serializable {

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
     * 司机 base_truck_driver.id
     */
    private Long driverId;

    /**
     * 运输任务ID
     */
    private Long taskTransportId;

    /**
     * 起始机构
     */
    private Long startOrgId;

    /**
     * 目的机构
     */
    private Long endOrgId;

    /**
     * 作业状态：1待执行 2进行中 3改派 4已完成 5已作废
     */
    private Integer status;

    /**
     * 提货对接人
     */
    private String startHandover;

    /**
     * 交付对接人
     */
    private String finishHandover;

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
