package com.xinfei.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xinfei.entity.DriverJob;
import com.xinfei.entity.DriverSettlement;
import com.xinfei.entity.DriverSettlementDetail;
import com.xinfei.mapper.DriverJobMapper;
import com.xinfei.mapper.DriverSettlementDetailMapper;
import com.xinfei.mapper.DriverSettlementMapper;
import com.xinfei.service.IDriverSettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 司机趟次结算 Service 实现（FIN-2，《21》§FIN-2 第一期定稿 2026-10-20）
 *
 * <p>趟次=司机结算天然单位：归集 work_driver_job.status=4 按 actualArrivalTime∈period；
 * 每趟金额=yml {@code xinfei.settlement.trip-fee}（缺省 0，禁硬编码），
 * 底薪/补贴/扣款为生成时手工调差项；应发=底薪+趟次费+补贴-扣款。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DriverSettlementServiceImpl extends ServiceImpl<DriverSettlementMapper, DriverSettlement>
        implements IDriverSettlementService {

    /** 司机作业单已完成（DriverJob.status=4，《07》§四） */
    private static final int JOB_STATUS_FINISHED = 4;
    private static final DateTimeFormatter NO_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 每趟固定费率（yml xinfei.settlement.trip-fee，缺省 0） */
    @Value("${xinfei.settlement.trip-fee:0}")
    private BigDecimal tripFeePerTrip;

    private final DriverJobMapper driverJobMapper;
    private final DriverSettlementDetailMapper detailMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DriverSettlement generate(Long driverId, LocalDate periodStart, LocalDate periodEnd,
                                     BigDecimal baseWage, BigDecimal allowance,
                                     BigDecimal deduction, String remark, Long operatorId) {
        if (driverId == null || periodStart == null || periodEnd == null) {
            throw new IllegalArgumentException("司机/账期起止必填");
        }
        if (periodStart.isAfter(periodEnd)) {
            throw new IllegalArgumentException("账期起不能晚于账期止");
        }
        baseWage = nonNegative(baseWage, "底薪");
        allowance = nonNegative(allowance, "补贴");
        deduction = nonNegative(deduction, "扣款");

        // 幂等：同司机账期重叠的未删结算单存在则拒绝并返回已有单号
        DriverSettlement existing = baseMapper.selectOne(new LambdaQueryWrapper<DriverSettlement>()
                .eq(DriverSettlement::getDriverId, driverId)
                .le(DriverSettlement::getPeriodStart, periodEnd)
                .ge(DriverSettlement::getPeriodEnd, periodStart)
                .last("LIMIT 1"));
        if (existing != null) {
            throw new IllegalArgumentException("该司机在账期内已有结算单: " + existing.getSettleNo());
        }

        List<DriverJob> jobs = collectableJobs(List.of(driverId), periodStart, periodEnd);
        if (jobs.isEmpty()) {
            throw new IllegalArgumentException("该司机在账期内无可结算趟次");
        }

        BigDecimal tripFee = tripFeePerTrip == null ? BigDecimal.ZERO : tripFeePerTrip;
        BigDecimal total = baseWage.add(tripFee.multiply(BigDecimal.valueOf(jobs.size())))
                .add(allowance).subtract(deduction);
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("应发金额不能为负，请核对扣款");
        }

        LocalDateTime now = LocalDateTime.now();
        DriverSettlement settlement = new DriverSettlement();
        settlement.setSettleNo(nextSettleNo(now));
        settlement.setDriverId(driverId);
        settlement.setPeriodStart(periodStart);
        settlement.setPeriodEnd(periodEnd);
        settlement.setTripCount(jobs.size());
        settlement.setBaseWage(baseWage);
        settlement.setTripFee(tripFee.multiply(BigDecimal.valueOf(jobs.size())));
        settlement.setAllowance(allowance);
        settlement.setDeduction(deduction);
        settlement.setTotalAmount(total);
        settlement.setStatus(DriverSettlement.STATUS_PENDING);
        settlement.setRemark(remark);
        settlement.setCreateBy(operatorId);
        settlement.setUpdateBy(operatorId);
        settlement.setCreateTime(now);
        settlement.setUpdateTime(now);
        baseMapper.insert(settlement);

        List<DriverSettlementDetail> details = new ArrayList<>(jobs.size());
        for (DriverJob job : jobs) {
            DriverSettlementDetail d = new DriverSettlementDetail();
            d.setSettlementId(settlement.getId());
            d.setTaskTransportId(job.getTaskTransportId());
            d.setDriverJobId(job.getId());
            d.setAmount(tripFee);
            d.setCreateBy(operatorId);
            d.setUpdateBy(operatorId);
            d.setCreateTime(now);
            d.setUpdateTime(now);
            details.add(d);
        }
        details.forEach(detailMapper::insert);
        log.info("[司机结算] 生成成功: settleNo={}, driverId={}, period={}~{}, trips={}, total={}",
                settlement.getSettleNo(), driverId, periodStart, periodEnd, jobs.size(), total);
        return settlement;
    }

    @Override
    public List<Map<String, Object>> candidateDrivers(LocalDate periodStart, LocalDate periodEnd) {
        if (periodStart == null || periodEnd == null) {
            throw new IllegalArgumentException("账期起止必填");
        }
        List<DriverJob> jobs = collectableJobs(null, periodStart, periodEnd);
        return jobs.stream().collect(Collectors.groupingBy(DriverJob::getDriverId, Collectors.counting()))
                .entrySet().stream()
                .map(e -> {
                    Map<String, Object> vo = new HashMap<>();
                    vo.put("driverId", e.getKey());
                    vo.put("tripCount", e.getValue().intValue());
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    public Page<DriverSettlement> page(Integer page, Integer pageSize, String settleNo, Long driverId, Integer status) {
        Page<DriverSettlement> p = new Page<>(page == null || page < 1 ? 1 : page,
                pageSize == null || pageSize < 1 ? 10 : pageSize);
        LambdaQueryWrapper<DriverSettlement> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(settleNo != null && !settleNo.isBlank(), DriverSettlement::getSettleNo, settleNo)
                .eq(driverId != null, DriverSettlement::getDriverId, driverId)
                .eq(status != null, DriverSettlement::getStatus, status)
                .orderByDesc(DriverSettlement::getCreateTime);
        return baseMapper.selectPage(p, wrapper);
    }

    @Override
    public Map<String, Object> detail(Long id) {
        DriverSettlement settlement = baseMapper.selectById(id);
        if (settlement == null) {
            throw new IllegalArgumentException("结算单不存在: " + id);
        }
        Map<String, Object> vo = new HashMap<>();
        vo.put("settlement", settlement);
        vo.put("details", listDetails(id));
        return vo;
    }

    @Override
    public boolean audit(Long id, String auditRemark, Long operatorId) {
        LambdaUpdateWrapper<DriverSettlement> uw = new LambdaUpdateWrapper<DriverSettlement>()
                .eq(DriverSettlement::getId, id)
                .eq(DriverSettlement::getStatus, DriverSettlement.STATUS_PENDING)
                .set(DriverSettlement::getStatus, DriverSettlement.STATUS_AUDITED)
                .set(DriverSettlement::getAuditRemark, auditRemark)
                .set(DriverSettlement::getUpdateBy, operatorId)
                .set(DriverSettlement::getUpdateTime, LocalDateTime.now());
        boolean ok = baseMapper.update(null, uw) > 0;
        if (ok) {
            log.info("[司机结算] 审核通过: id={}, operator={}", id, operatorId);
        }
        return ok;
    }

    @Override
    public boolean pay(Long id, Long operatorId) {
        LambdaUpdateWrapper<DriverSettlement> uw = new LambdaUpdateWrapper<DriverSettlement>()
                .eq(DriverSettlement::getId, id)
                .eq(DriverSettlement::getStatus, DriverSettlement.STATUS_AUDITED)
                .set(DriverSettlement::getStatus, DriverSettlement.STATUS_PAID)
                .set(DriverSettlement::getPaidTime, LocalDateTime.now())
                .set(DriverSettlement::getUpdateBy, operatorId)
                .set(DriverSettlement::getUpdateTime, LocalDateTime.now());
        boolean ok = baseMapper.update(null, uw) > 0;
        if (ok) {
            log.info("[司机结算] 发放登记: id={}, operator={}", id, operatorId);
        }
        return ok;
    }

    @Override
    public List<DriverSettlementDetail> listDetails(Long settlementId) {
        return detailMapper.selectList(new LambdaQueryWrapper<DriverSettlementDetail>()
                .eq(DriverSettlementDetail::getSettlementId, settlementId)
                .orderByAsc(DriverSettlementDetail::getId));
    }

    /**
     * 归集可结算趟次：status=4 + actualArrivalTime∈period + 未被任何未删结算单引用；
     * driverIds 为 null 时查全部司机。
     */
    private List<DriverJob> collectableJobs(List<Long> driverIds, LocalDate periodStart, LocalDate periodEnd) {
        LambdaQueryWrapper<DriverJob> wrapper = new LambdaQueryWrapper<DriverJob>()
                .eq(DriverJob::getStatus, JOB_STATUS_FINISHED)
                .ge(DriverJob::getActualArrivalTime, periodStart.atStartOfDay())
                .le(DriverJob::getActualArrivalTime, periodEnd.atTime(23, 59, 59));
        if (driverIds != null && !driverIds.isEmpty()) {
            wrapper.in(DriverJob::getDriverId, driverIds);
        }
        List<DriverJob> jobs = driverJobMapper.selectList(wrapper);
        if (jobs.isEmpty()) {
            return jobs;
        }
        // 排除已被未删结算单引用的作业单（uk(settlement_id,driver_job_id) 兜底）
        List<Long> jobIds = jobs.stream().map(DriverJob::getId).collect(Collectors.toList());
        Set<Long> used = detailMapper.selectList(new LambdaQueryWrapper<DriverSettlementDetail>()
                        .in(DriverSettlementDetail::getDriverJobId, jobIds))
                .stream().map(DriverSettlementDetail::getDriverJobId).collect(Collectors.toSet());
        return jobs.stream().filter(j -> !used.contains(j.getId())).collect(Collectors.toList());
    }

    /** 结算单号：DJS+yyyyMMdd+当日序号（按前缀计数+1） */
    private String nextSettleNo(LocalDateTime now) {
        String prefix = "DJS" + now.format(NO_DATE);
        Long count = baseMapper.selectCount(new LambdaQueryWrapper<DriverSettlement>()
                .likeRight(DriverSettlement::getSettleNo, prefix));
        return prefix + String.format("%03d", (count == null ? 0 : count) + 1);
    }

    private BigDecimal nonNegative(BigDecimal v, String label) {
        if (v == null) {
            return BigDecimal.ZERO;
        }
        if (v.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(label + "不能为负");
        }
        return v;
    }
}
