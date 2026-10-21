package com.xinfei.DTO.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 管理端大屏/仪表盘-运输域汇总统计。
 *
 * <p>车辆状态口径（OPS-3，禁止给 base_truck 加作业状态字段，状态由运输任务关联推导）：</p>
 * <ul>
 *   <li>在途车辆 inTransitTruckCount：拥有进行中(status=2)运输任务的去重车辆数；</li>
 *   <li>待执行车辆 pendingTruckCount：拥有待执行(1)/待确认(3)运输任务的去重车辆数；</li>
 *   <li>空闲车辆由 BFF 用「正常车辆总数 - 在途 - 待执行」推导，不在本域统计。</li>
 * </ul>
 */
@Data
@Schema(description = "运输域汇总统计")
public class TaskDashboardSummaryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "今日新增运单数（work_transport_order 按 create_time 当日）")
    private Long todayTransportOrderCount;

    @Schema(description = "今日新增运输任务数（work_task_transport 按 create_time 当日）")
    private Long todayTaskCount;

    @Schema(description = "在途车辆数（有进行中任务的去重车辆数）")
    private Long inTransitTruckCount;

    @Schema(description = "待执行车辆数（有待执行/待确认任务的去重车辆数）")
    private Long pendingTruckCount;

    @Schema(description = "运输任务按 5 态分组计数（固定顺序，零值补 0）")
    private List<NameValueDTO> taskStatusCounts;

    /**
     * 准时率（百分比，0~100，整数；无已送达任务为 null）。
     * 口径：已送达任务（actual_arrival_time 非空）中实际到达 <= 计划到达的占比，
     * 与司机绩效「准点率」一致（docs/需求文档/20 全局聚合版）。
     */
    private Integer onTimeRate;
}
