package com.xinfei.feign.fallback;

import com.xinfei.common.utils.Result;
import com.xinfei.feign.DriverSettlementFeign;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 司机结算 Feign 熔断降级（FIN-2）
 */
@Slf4j
@Component
public class DriverSettlementFeignFallback implements DriverSettlementFeign {

    @Override
    public Result generate(Long driverId, String periodStart, String periodEnd,
                           BigDecimal baseWage, BigDecimal allowance, BigDecimal deduction, String remark) {
        log.warn("远程调用 xfwl-work 失败: generate(driverId={}, period={}~{})", driverId, periodStart, periodEnd);
        return Result.error("服务降级，结算单生成失败");
    }

    @Override
    public Result drivers(String periodStart, String periodEnd) {
        log.warn("远程调用 xfwl-work 失败: drivers(period={}~{})", periodStart, periodEnd);
        return Result.error("服务降级，候选司机查询失败");
    }

    @Override
    public Result page(Integer page, Integer pageSize, String settleNo, Long driverId, Integer status,
                       String periodStart, String periodEnd) {
        log.warn("远程调用 xfwl-work 失败: page(settleNo={})", settleNo);
        return Result.error("服务降级，结算单分页失败");
    }

    @Override
    public Result detail(Long id) {
        log.warn("远程调用 xfwl-work 失败: detail(id={})", id);
        return Result.error("服务降级，结算单详情失败");
    }

    @Override
    public Result audit(Long id, String auditRemark) {
        log.warn("远程调用 xfwl-work 失败: audit(id={})", id);
        return Result.error("服务降级，审核失败");
    }

    @Override
    public Result pay(Long id) {
        log.warn("远程调用 xfwl-work 失败: pay(id={})", id);
        return Result.error("服务降级，发放登记失败");
    }
}
