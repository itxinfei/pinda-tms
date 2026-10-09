package com.itheima.pinda.service;

/**
 * 结算服务
 *
 * <p>订单签收后生成运费明细与结算单，打通"运费算出 → 入账 → 对账"环节。
 * 事件处理幂等：同一订单重复触发不会重复生成。</p>
 */
public interface SettlementService {

    /**
     * 订单签收结算：生成运费明细与应收结算单（状态=待对账）
     *
     * @param orderId           订单id
     * @param transportOrderId  运单id（可为空）
     */
    void settle(String orderId, String transportOrderId);
}
