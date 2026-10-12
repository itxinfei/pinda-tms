package com.itheima.pinda.vo.dashboard;

import com.itheima.pinda.DTO.dashboard.NameValueDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 运营可视化大屏汇总聚合（BFF，不连库）。
 *
 * <p>订单来自 pd-oms；运单/任务/车辆状态/车次运量来自 pd-work；车辆总数来自 pd-base，
 * 空闲车辆 = 总数 - 在途 - 待执行（下限 0）。告警数由前端直连 pd-netty /alarm 取分页 total，
 * 车辆定位散点由前端直连 pd-netty /trace/page 获取，均不经本服务。</p>
 */
@Data
@Schema(description = "运营大屏汇总聚合")
public class BigScreenSummaryVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "今日订单数")
    private Long todayOrderCount;

    @Schema(description = "今日运单数（去重 order_id）")
    private Long todayTransportOrderCount;

    @Schema(description = "今日运输任务数")
    private Long todayTaskCount;

    @Schema(description = "在途车辆数")
    private Long activeTruckCount;

    @Schema(description = "待执行车辆数")
    private Long pendingTruckCount;

    @Schema(description = "空闲车辆数（正常车辆总数-在途-待执行，下限0）")
    private Long idleTruckCount;

    @Schema(description = "正常车辆总数")
    private Long totalTruckCount;

    @Schema(description = "运输任务五态计数")
    private List<NameValueDTO> taskStatusCounts;

    @Schema(description = "近7日各线路运量 Top10")
    private List<LineVolumeVo> lineVolumes;
}
