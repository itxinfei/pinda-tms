package com.xinfei.service;

import com.xinfei.DTO.dashboard.OrderOverviewDTO;
import com.xinfei.DTO.dashboard.OrderTrendDTO;
import com.xinfei.DTO.dashboard.PendingOrderDTO;

import java.util.List;

/**
 * 管理端工作台-订单统计只读服务（不承载交易/写操作）。
 */
public interface IOrderDashboardService {

    /**
     * 订单总览：今日新增 + 待处理/运输中/已签收三桶计数。
     *
     * @return 总览统计
     */
    OrderOverviewDTO overview();

    /**
     * 近 N 日运输量趋势（下单量 + 签收量），缺日补 0，按日期升序。
     *
     * @param days 天数（服务端钳制到 1~30）
     * @return 每日趋势点
     */
    List<OrderTrendDTO> trend(Integer days);

    /**
     * 最新待处理运单（状态 1~5，按 id 倒序）。
     *
     * @param limit 返回条数（服务端钳制到 1~20）
     * @return 待处理运单列表
     */
    List<PendingOrderDTO> pendingOrders(Integer limit);
}
