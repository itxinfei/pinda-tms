package com.xinfei.vo.dashboard;

import com.xinfei.DTO.dashboard.NameValueDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 管理端工作台-顶部 KPI 与运单状态概览聚合。
 *
 * <p>数据来源：订单四项+今日签收来自 pd-oms；在途/待执行车辆与任务五态来自 pd-work
 * （车辆状态按 OPS-3 由运输任务关联推导）。</p>
 */
@Data
@Schema(description = "工作台总览聚合")
public class DashboardOverviewVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "今日新增订单数")
    private Long todayOrderCount;

    @Schema(description = "在途车辆数（有进行中任务的去重车辆数）")
    private Long onTruckCount;

    @Schema(description = "待处理订单数（订单状态1-5）")
    private Long pendingCount;

    @Schema(description = "运输中订单数（订单状态6-9）")
    private Long inTransitCount;

    @Schema(description = "已签收订单数（订单状态10）")
    private Long completedCount;

    @Schema(description = "今日签收数（update_time 当日且状态10近似）")
    private Long todayCompletedCount;

    @Schema(description = "今日新增运输任务数")
    private Long todayTaskCount;

    @Schema(description = "运输任务五态计数（待执行/进行中/待确认/已完成/已取消）")
    private List<NameValueDTO> taskStatusCounts;
}
