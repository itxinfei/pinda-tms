package com.xinfei.feign.fallback;

import com.xinfei.common.utils.Result;
import com.xinfei.feign.DecisionLogFeign;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 合规决策留痕 Feign 熔断降级（留痕属合规要求，降级即告警——上游应阻断决策并人工补录）
 */
@Slf4j
@Component
public class DecisionLogFeignFallback implements DecisionLogFeign {

    @Override
    public Result apply(String businessType, Long businessId, String reasonEnum, String detail) {
        log.error("远程调用 xfwl-oms 失败: decision-log apply(type={}, bizId={})——合规留痕失败，上游应阻断决策", businessType, businessId);
        return Result.error("服务降级，决策留痕失败");
    }

    @Override
    public Result page(Integer page, Integer pageSize, String businessType, Long businessId) {
        log.warn("远程调用 xfwl-oms 失败: decision-log page");
        return Result.error("服务降级，留痕查询失败");
    }

    @Override
    public Result export(String businessType, Long businessId, String beginTime, String endTime) {
        log.warn("远程调用 xfwl-oms 失败: decision-log export");
        return Result.error("服务降级，留痕导出失败");
    }
}
