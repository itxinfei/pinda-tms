package com.xinfei.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinfei.entity.DriverSettlement;
import com.xinfei.entity.DriverSettlementDetail;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 司机趟次结算 Service（FIN-2，《21》§FIN-2 第一期定稿）
 */
public interface IDriverSettlementService {

    /**
     * 按司机+账期生成结算单（归集周期内 work_driver_job.status=4 且未被未删结算单引用的趟次）
     *
     * @throws IllegalArgumentException 参数错/无可归集趟次/同司机期间重叠已有单（幂等拒绝，message 带已有单号）
     */
    DriverSettlement generate(Long driverId, LocalDate periodStart, LocalDate periodEnd,
                              java.math.BigDecimal baseWage, java.math.BigDecimal allowance,
                              java.math.BigDecimal deduction, String remark, Long operatorId);

    /**
     * 候选司机：账期内有可归集趟次（status=4 且未被引用）的司机清单（distinct driverId + tripCount）
     */
    List<Map<String, Object>> candidateDrivers(LocalDate periodStart, LocalDate periodEnd);

    /**
     * 结算单分页（settleNo 模糊 / driverId / status / 账期重叠过滤，orderByDesc createTime）
     *
     * @param periodStart 账期筛选起（与结算单账期取交集，可空）
     * @param periodEnd   账期筛选止（可空）
     */
    Page<DriverSettlement> page(Integer page, Integer pageSize, String settleNo, Long driverId, Integer status,
                                LocalDate periodStart, LocalDate periodEnd);

    /**
     * 详情：结算单头 + 逐趟明细
     */
    Map<String, Object> detail(Long id);

    /**
     * 审核 0→1（条件 UPDATE，状态已变化返回 false）
     */
    boolean audit(Long id, String auditRemark, Long operatorId);

    /**
     * 发放 1→2（线下打款后登记，写 paid_time；条件 UPDATE，状态已变化返回 false）
     */
    boolean pay(Long id, Long operatorId);

    /**
     * 明细列表（详情页用）
     */
    List<DriverSettlementDetail> listDetails(Long settlementId);
}
