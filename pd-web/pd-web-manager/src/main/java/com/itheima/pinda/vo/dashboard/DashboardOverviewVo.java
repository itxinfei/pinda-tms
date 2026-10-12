package com.itheima.pinda.vo.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 管理端工作台-顶部 KPI 与运单状态概览聚合。
 *
 * <p>数据来源：订单四项来自 pd-oms；在途车辆数来自 pd-work 进行中(status=2)运输任务计数。
 * 准时率无后端数据源（DIS-04/DSP-05 待建），不下发该字段，由前端展示占位。</p>
 */
@Data
@Schema(description = "工作台总览聚合")
public class DashboardOverviewVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "今日新增运单数")
    private Long todayOrderCount;

    @Schema(description = "在途车辆数（进行中运输任务数）")
    private Long onTruckCount;

    @Schema(description = "待处理运单数（订单状态1-5）")
    private Long pendingCount;

    @Schema(description = "运输中运单数（订单状态6-9）")
    private Long inTransitCount;

    @Schema(description = "已签收运单数（订单状态10）")
    private Long completedCount;
}
