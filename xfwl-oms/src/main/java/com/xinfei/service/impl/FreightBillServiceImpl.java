package com.xinfei.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xinfei.common.CustomIdGenerator;
import com.xinfei.entity.FreightBill;
import com.xinfei.entity.SettlementOrder;
import com.xinfei.mapper.FreightBillMapper;
import com.xinfei.service.IFreightBillService;
import com.xinfei.service.ISettlementOrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 客户运费账单 Service 实现（P0-6b 应收对账闭环，据《21》§1.5）
 *
 * <p>核心不变量：账单是归集视图，明细即 {@link SettlementOrder}（不建明细表、不双写金额）；
 * 金额只读结算单 receivable_amount 汇总，调账一律走调整记录，禁止 UPDATE 原金额。</p>
 */
@Slf4j
@Service
public class FreightBillServiceImpl extends ServiceImpl<FreightBillMapper, FreightBill>
        implements IFreightBillService {

    @Autowired
    private ISettlementOrderService settlementOrderService;

    @Autowired
    private CustomIdGenerator idGenerator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FreightBill generate(String customerId, LocalDate periodStart, LocalDate periodEnd, Long operatorId) {
        if (customerId == null || customerId.isBlank() || periodStart == null || periodEnd == null) {
            throw new IllegalArgumentException("客户id与账期起止不能为空");
        }
        if (periodEnd.isBefore(periodStart)) {
            throw new IllegalArgumentException("账期起止不合法（结束早于开始）");
        }

        // ① 幂等：同客户同账期已有未核销账单则拒绝（不重复归集）
        Long existCount = baseMapper.selectCount(new LambdaQueryWrapper<FreightBill>()
                .eq(FreightBill::getCustomerId, customerId)
                .eq(FreightBill::getPeriodStart, periodStart)
                .eq(FreightBill::getPeriodEnd, periodEnd)
                .ne(FreightBill::getStatus, FreightBill.STATUS_WRITTEN_OFF));
        if (existCount != null && existCount > 0) {
            log.warn("[账单] 同客户同账期已存在未核销账单，拒绝重复生成: customerId={}, period={}~{}",
                    customerId, periodStart, periodEnd);
            return null;
        }

        // ② 归集：该客户 应收 且 status∈{待对账,已对账} 且未归账(bill_id 为空) 且签收日落在账期内的结算单
        List<SettlementOrder> settlements = settlementOrderService.lambdaQuery()
                .eq(SettlementOrder::getSettleObjectId, customerId)
                .eq(SettlementOrder::getDirection, SettlementOrder.DIRECTION_RECEIVABLE)
                .in(SettlementOrder::getStatus, SettlementOrder.STATUS_PENDING, SettlementOrder.STATUS_RECONCILED)
                .isNull(SettlementOrder::getBillId)
                .ge(SettlementOrder::getPeriodStart, periodStart)
                .le(SettlementOrder::getPeriodStart, periodEnd)
                .list();
        if (settlements.isEmpty()) {
            log.warn("[账单] 账期内无可归集的应收结算单: customerId={}, period={}~{}", customerId, periodStart, periodEnd);
            return null;
        }

        // ③ 落账单（雪花id预生成，bill_no 落库前确定）
        Long billId = idGenerator.nextId(new FreightBill());
        BigDecimal total = settlements.stream()
                .map(s -> s.getReceivableAmount() == null ? BigDecimal.ZERO : s.getReceivableAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDateTime now = LocalDateTime.now();
        FreightBill bill = new FreightBill();
        bill.setId(billId);
        bill.setBillNo("FB" + billId);
        bill.setCustomerId(customerId);
        bill.setPeriodStart(periodStart);
        bill.setPeriodEnd(periodEnd);
        bill.setOrderCount(settlements.size());
        bill.setTotalAmount(total);
        bill.setStatus(FreightBill.STATUS_PENDING);
        bill.setCreateBy(operatorId);
        bill.setCreateTime(now);
        bill.setUpdateBy(operatorId);
        bill.setUpdateTime(now);
        baseMapper.insert(bill);

        // ④ 回写结算单 bill_id（只回写本次归集的、仍为待对账/已对账且未归账的，避免覆盖他人并发归集）
        for (SettlementOrder s : settlements) {
            SettlementOrder upd = new SettlementOrder();
            upd.setId(s.getId());
            upd.setBillId(billId);
            upd.setUpdateTime(now);
            boolean ok = settlementOrderService.lambdaUpdate()
                    .eq(SettlementOrder::getId, s.getId())
                    .isNull(SettlementOrder::getBillId)
                    .update(upd);
            if (!ok) {
                log.warn("[账单] 结算单 bill_id 回写未命中(可能已被并发归集): settlementId={}", s.getId());
            }
        }
        log.info("[账单] 生成成功: billNo={}, customerId={}, 归集结算单={} 笔, 应收总额={}",
                bill.getBillNo(), customerId, settlements.size(), total);
        return bill;
    }

    @Override
    public Map<String, Object> reconcile(Long billId) {
        FreightBill bill = baseMapper.selectById(billId);
        if (bill == null) {
            return null;
        }
        List<SettlementOrder> settlements = listSettlements(billId);
        BigDecimal sum = settlements.stream()
                .map(s -> s.getReceivableAmount() == null ? BigDecimal.ZERO : s.getReceivableAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal billAmount = bill.getTotalAmount() == null ? BigDecimal.ZERO : bill.getTotalAmount();
        // 差异标记：账单应收 vs Σ结算单应收（应恒为 0；非 0 仅展示，不自动改）
        BigDecimal diff = billAmount.subtract(sum);

        Map<String, Object> vo = new HashMap<>();
        vo.put("bill", bill);
        vo.put("settlements", settlements);
        vo.put("sumReceivable", sum);
        vo.put("diff", diff);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean confirm(Long billId, Long operatorId) {
        FreightBill bill = baseMapper.selectById(billId);
        if (bill == null || bill.getStatus() == null || bill.getStatus() != FreightBill.STATUS_PENDING) {
            log.warn("[账单] 确认失败：账单不存在或状态非待确认: billId={}, status={}", billId, bill == null ? null : bill.getStatus());
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        FreightBill upd = new FreightBill();
        upd.setId(billId);
        upd.setStatus(FreightBill.STATUS_CONFIRMED);
        upd.setUpdateBy(operatorId);
        upd.setUpdateTime(now);
        baseMapper.updateById(upd);

        // 联动：其下 待对账(0) 结算单 → 已对账(1)
        settlementOrderService.lambdaUpdate()
                .eq(SettlementOrder::getBillId, billId)
                .eq(SettlementOrder::getStatus, SettlementOrder.STATUS_PENDING)
                .set(SettlementOrder::getStatus, SettlementOrder.STATUS_RECONCILED)
                .set(SettlementOrder::getUpdateTime, now)
                .update();
        log.info("[账单] 确认成功: billId={} (0→1，结算单联动 0→1)", billId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean writeOff(Long billId, Long operatorId) {
        FreightBill bill = baseMapper.selectById(billId);
        if (bill == null || bill.getStatus() == null || bill.getStatus() != FreightBill.STATUS_CONFIRMED) {
            log.warn("[账单] 核销失败：账单不存在或状态非已确认: billId={}, status={}", billId, bill == null ? null : bill.getStatus());
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        FreightBill upd = new FreightBill();
        upd.setId(billId);
        upd.setStatus(FreightBill.STATUS_WRITTEN_OFF);
        upd.setUpdateBy(operatorId);
        upd.setUpdateTime(now);
        baseMapper.updateById(upd);

        // 联动：其下 已对账(1) 结算单 → 已结算(2)
        settlementOrderService.lambdaUpdate()
                .eq(SettlementOrder::getBillId, billId)
                .eq(SettlementOrder::getStatus, SettlementOrder.STATUS_RECONCILED)
                .set(SettlementOrder::getStatus, SettlementOrder.STATUS_SETTLED)
                .set(SettlementOrder::getUpdateTime, now)
                .update();
        log.info("[账单] 核销成功: billId={} (1→2，结算单联动 1→2)", billId);
        return true;
    }

    @Override
    public List<SettlementOrder> listSettlements(Long billId) {
        return settlementOrderService.lambdaQuery()
                .eq(SettlementOrder::getBillId, billId)
                .orderByDesc(SettlementOrder::getCreateTime)
                .list();
    }

    @Override
    public IPage<FreightBill> page(Integer page, Integer pageSize, String billNo, String customerId, Integer status) {
        return lambdaQuery()
                .like(StringUtils.isNotBlank(billNo), FreightBill::getBillNo, billNo)
                .eq(StringUtils.isNotBlank(customerId), FreightBill::getCustomerId, customerId)
                .eq(status != null, FreightBill::getStatus, status)
                .orderByDesc(FreightBill::getCreateTime)
                .page(new Page<>(page == null || page < 1 ? 1 : page, pageSize == null || pageSize < 1 ? 10 : pageSize));
    }
}
