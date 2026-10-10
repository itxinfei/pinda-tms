package com.itheima.pinda.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.pinda.common.CustomIdGenerator;
import com.itheima.pinda.common.utils.IdConverter;
import com.itheima.pinda.entity.FreightDetail;
import com.itheima.pinda.entity.Order;
import com.itheima.pinda.entity.SettlementOrder;
import com.itheima.pinda.service.IFreightDetailService;
import com.itheima.pinda.service.IOrderService;
import com.itheima.pinda.service.ISettlementOrderService;
import com.itheima.pinda.service.SettlementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 结算服务实现
 *
 * <p>最小可落地闭环：订单签收 → 落一条运费明细（订单总价）→ 生成应收结算单（待对账）。</p>
 *
 * <p>事务采用 REQUIRES_NEW：结算独立成事务，结算异常仅回滚结算写入，
 * 不污染签收主流程；事件/签收双触发时靠"先查后建 + 唯一索引"保证幂等。</p>
 *
 * <p>外部依赖边界（本次不实现，需外部资质）：
 * 真实微信/支付宝收款、数电票开具、银行付款，均由外部渠道网关对接，
 * 本服务只生成内部应收单据，不触达任何外部资金/税控接口。</p>
 */
@Slf4j
@Service
public class SettlementServiceImpl implements SettlementService {

    @Autowired
    private ISettlementOrderService settlementOrderService;

    @Autowired
    private IFreightDetailService freightDetailService;

    @Autowired
    private IOrderService orderService;

    @Autowired
    private CustomIdGenerator idGenerator;

    /**
     * 订单签收结算
     *
     * <p>异常不外抛：结算失败只回滚本结算事务并记录日志，
     * 签收结果不受影响；后续可通过重新签收事件或人工补偿再次触发（幂等）。</p>
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void settle(String orderId, String transportOrderId) {
        try {
            doSettle(orderId, transportOrderId);
        } catch (Exception e) {
            log.error("[结算] 订单[{}]结算处理异常，回滚结算事务（不影响签收）", orderId, e);
            // 标记当前(REQUIRES_NEW)事务回滚，不向外抛出以免污染调用方事务
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        }
    }

    /**
     * 实际结算逻辑（幂等）
     */
    private void doSettle(String orderId, String transportOrderId) {
        Long orderIdValue = IdConverter.toLong(orderId);
        if (orderIdValue == null) {
            log.warn("[结算] 订单id为空，跳过结算");
            return;
        }
        Long transportOrderIdValue = IdConverter.toLong(transportOrderId);

        // 幂等：结算单已存在（重复交付事件/重复状态更新）直接返回
        SettlementOrder existing = settlementOrderService.getOne(
                new LambdaQueryWrapper<SettlementOrder>()
                        .eq(SettlementOrder::getOrderId, orderIdValue));
        if (existing != null) {
            log.info("[结算] 订单[{}]结算单已存在，幂等跳过: settlementNo={}", orderId, existing.getSettlementNo());
            return;
        }

        Order order = orderService.getById(orderIdValue);
        if (order == null) {
            log.warn("[结算] 订单不存在，跳过结算: orderId={}", orderId);
            return;
        }
        BigDecimal amount = order.getAmount() == null ? BigDecimal.ZERO : order.getAmount();
        LocalDateTime now = LocalDateTime.now();

        // ① 运费明细：最小闭环先按"总价 + 默认费用项(运费)"落一条
        //    后续 Drools 规则扩展后，在此按费用项拆分首重/续重/保价/上楼等多条明细
        FreightDetail existDetail = freightDetailService.getOne(
                new LambdaQueryWrapper<FreightDetail>()
                        .eq(FreightDetail::getOrderId, orderIdValue)
                        .eq(FreightDetail::getFeeItem, FreightDetail.FEE_ITEM_FREIGHT));
        if (existDetail == null) {
            FreightDetail detail = new FreightDetail();
            detail.setOrderId(orderIdValue);
            detail.setTransportOrderId(transportOrderIdValue);
            detail.setFeeItem(FreightDetail.FEE_ITEM_FREIGHT);
            detail.setFeeItemName(FreightDetail.FEE_ITEM_NAME_FREIGHT);
            detail.setQuantity(BigDecimal.ONE);
            detail.setUnit(FreightDetail.UNIT_TICKET);
            detail.setUnitPrice(amount);
            detail.setAmount(amount);
            detail.setCreateTime(now);
            // id 走 ASSIGN_ID 自动雪花
            freightDetailService.save(detail);
        }

        // ② 结算单：应收客户、状态=待对账，应收金额=订单金额
        //    settlement_no 非空且需在落库前确定，用雪花id预生成并显式赋值（ASSIGN_ID 不覆盖非空id）
        Long settlementId = idGenerator.nextId(new SettlementOrder());
        SettlementOrder settlement = new SettlementOrder();
        settlement.setId(settlementId);
        settlement.setSettlementNo("STL" + settlementId);
        settlement.setOrderId(orderIdValue);
        settlement.setTransportOrderId(transportOrderIdValue);
        settlement.setSettleObjectType(SettlementOrder.OBJECT_MEMBER);
        // 散客无 memberId 时兜底，避免非空约束写入失败
        settlement.setSettleObjectId(order.getMemberId() == null
                ? SettlementOrder.GUEST_OBJECT_ID
                : String.valueOf(order.getMemberId()));
        settlement.setDirection(SettlementOrder.DIRECTION_RECEIVABLE);
        settlement.setPeriodStart(now.toLocalDate());
        settlement.setPeriodEnd(now.toLocalDate());
        settlement.setOrderCount(1);
        settlement.setReceivableAmount(amount);
        settlement.setPayableAmount(BigDecimal.ZERO);
        settlement.setSettledAmount(BigDecimal.ZERO);
        settlement.setStatus(SettlementOrder.STATUS_PENDING);
        settlement.setCreateTime(now);
        settlement.setUpdateTime(now);
        settlementOrderService.save(settlement);

        log.info("[结算] 订单[{}]结算单已生成: settlementNo={}, 应收金额={}",
                orderId, settlement.getSettlementNo(), amount);
    }
}
