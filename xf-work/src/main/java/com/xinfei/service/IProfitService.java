package com.xinfei.service;

import java.util.List;
import java.util.Map;

/**
 * 利润查询 Service（P0-6d / FIN-4 "老板看数"）
 *
 * <p>V1 只做毛利（收入 − 直接成本），不做间接成本分摊；
 * 订单利润 V1 按趟内均摊（equal，§6.4a 精细分摊 P1）；
 * 收入 = Σ(趟关联订单 Order.amount)，经 OrderFeign 跨服聚合；利润本身不建表。</p>
 */
public interface IProfitService {

    /**
     * 单趟利润（收入、成本明细、毛利、毛利率）。
     */
    Map<String, Object> taskProfit(Long taskTransportId);

    /**
     * 单订单利润（V1：趟成本按趟内订单数均摊）。
     */
    Map<String, Object> orderProfit(Long orderId);

    /**
     * 利润汇总（按月聚合，近 200 趟）。
     *
     * @return [{month, tripCount, income, cost, profit}]，月份倒序
     */
    List<Map<String, Object>> summary();

    /**
     * 利润排行（近 200 趟，最赚 Top5 / 最亏 Bottom5）。
     *
     * @return {top: [...], bottom: [...]}
     */
    Map<String, Object> ranking();
}
