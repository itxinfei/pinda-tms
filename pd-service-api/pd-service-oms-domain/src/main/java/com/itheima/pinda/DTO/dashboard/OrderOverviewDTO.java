package com.itheima.pinda.DTO.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 管理端工作台-订单总览统计。
 *
 * <p>状态归并口径（依据《07-数据字典与枚举口径》OrderStatus 1~12，归并逻辑在 pd-oms 域完成，
 * 前端不做状态码判断）：</p>
 * <ul>
 *   <li>待处理 pendingCount：1 待取件 / 2 已取件 / 3 网点自寄 / 4 网点入库 / 5 待装车；</li>
 *   <li>运输中 inTransitCount：6 运输中 / 7 网点出库 / 8 待派送 / 9 派送中；</li>
 *   <li>已完成 completedCount：10 已签收。</li>
 * </ul>
 * <p>拒收(11)/已取消(12)为非正常终态，不计入上述三桶。今日运单按 create_time 当日统计。</p>
 */
@Data
@Schema(description = "工作台订单总览")
public class OrderOverviewDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "今日新增运单数")
    private Long todayCount;

    @Schema(description = "待处理运单数（状态1-5）")
    private Long pendingCount;

    @Schema(description = "运输中运单数（状态6-9）")
    private Long inTransitCount;

    @Schema(description = "已签收运单数（状态10）")
    private Long completedCount;

    @Schema(description = "今日签收数（按 update_time 当日且状态10近似，oms_order 无签收完成时间列）")
    private Long todayCompletedCount;
}
