package com.xinfei.DTO.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 管理端工作台-运输量趋势单点（某一天）。
 *
 * <p>口径说明：oms_order 无独立签收完成时间列，故运单量按 create_time 归属日期，
 * 完成量按 update_time + status=10(已签收) 归属日期，后者为近似值（签收后其他字段更新会刷新
 * update_time）。缺日期由 pd-oms 补 0，序列连续，前端直接绘图。</p>
 */
@Data
@Schema(description = "工作台运输量趋势点")
public class OrderTrendDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "日期（yyyy-MM-dd）")
    private String date;

    @Schema(description = "当日下单量")
    private Long orderCount;

    @Schema(description = "当日签收量（按 update_time 近似）")
    private Long completedCount;

    public OrderTrendDTO() {
    }

    public OrderTrendDTO(String date, Long orderCount, Long completedCount) {
        this.date = date;
        this.orderCount = orderCount;
        this.completedCount = completedCount;
    }
}
