package com.xinfei.feign.fallback;

import com.xinfei.common.utils.Result;
import com.xinfei.feign.FreightBillFeign;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 运费账单（对账）Feign 熔断降级
 */
@Slf4j
@Component
public class FreightBillFeignFallback implements FreightBillFeign {

    @Override
    public Result generate(String customerId, String periodStart, String periodEnd) {
        log.warn("远程调用 xf-oms 失败: generate(customerId={}, period={}~{})", customerId, periodStart, periodEnd);
        return Result.error("服务降级，账单生成失败");
    }

    @Override
    public Result reconcile(Long id) {
        log.warn("远程调用 xf-oms 失败: reconcile(id={})", id);
        return Result.error("服务降级，对账视图获取失败");
    }

    @Override
    public Result confirm(Long id) {
        log.warn("远程调用 xf-oms 失败: confirm(id={})", id);
        return Result.error("服务降级，账单确认失败");
    }

    @Override
    public Result writeOff(Long id) {
        log.warn("远程调用 xf-oms 失败: writeOff(id={})", id);
        return Result.error("服务降级，账单核销失败");
    }
}
