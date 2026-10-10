package com.itheima.pinda.vo.work;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.itheima.pinda.vo.oms.OrderVo;
import com.itheima.pinda.vo.base.angency.AgencySimpleVo;
import com.itheima.pinda.vo.base.userCenter.SysUserVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "取派件任务信息")
public class TaskPickupDispatchVo implements Serializable {
    private static final long serialVersionUID = -3299886291703490690L;

    @Schema(description = "id")
    private String id;

    @Schema(description = "订单信息")
    private OrderVo order;

    @Schema(description = "任务类型，1为取件任务，2为派件任务")
    private Integer taskType;

    @Schema(description = "任务状态，1为待执行（对应 待上门和须交接）、2为进行中（该状态暂不使用，属于保留状态）、3为待确认（对应 待妥投和须交件）、4为已完成、5为已取消")
    private Integer status;

    @Schema(description = "签收状态(1为已签收，2为拒收)")
    private Integer signStatus;

    @Schema(description = "任务所属网点信息")
    private AgencySimpleVo agency;

    @Schema(description = "快递员信息")
    private SysUserVo courier;

    @Schema(description = "预计开始时间")
    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss"
    )
    private LocalDateTime estimatedStartTime;

    @Schema(description = "实际开始时间")
    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss"
    )
    private LocalDateTime actualStartTime;

    @Schema(description = "预计完成时间")
    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss"
    )
    private LocalDateTime estimatedEndTime;

    @Schema(description = "实际完成时间")
    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss"
    )
    private LocalDateTime actualEndTime;

    @Schema(description = "确认时间")
    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss"
    )
    private LocalDateTime confirmTime;

    @Schema(description = "取消时间")
    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss"
    )
    private LocalDateTime cancelTime;

    @Schema(description = "任务分配状态(1未分配2已分配3待人工分配)")
    private Integer assignedStatus;

    @Schema(description = "备注")
    private String mark;

    @Schema(description = "任务创建时间")
    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss"
    )
    private LocalDateTime createTime;

    @Schema(description = "页码")
    private Integer page;

    @Schema(description = "页尺寸")
    private Integer pageSize;

    @Schema(description = "运单")
    private TransportOrderVo transportOrder;
}
