package com.xinfei.feign.fallback;

import com.xinfei.common.utils.Result;
import com.xinfei.feign.ProfitFeign;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 利润查询 Feign 熔断降级
 */
@Slf4j
@Component
public class ProfitFeignFallback implements ProfitFeign {

    @Override
    public Result taskProfit(Long taskId) {
        log.warn("远程调用 xfwl-work 失败: taskProfit(taskId={})", taskId);
        return Result.error("服务降级，趟利润查询失败");
    }

    @Override
    public Result orderProfit(Long orderId) {
        log.warn("远程调用 xfwl-work 失败: orderProfit(orderId={})", orderId);
        return Result.error("服务降级，订单利润查询失败");
    }

    @Override
    public Result summary() {
        log.warn("远程调用 xfwl-work 失败: summary()");
        return Result.error("服务降级，利润汇总查询失败");
    }

    @Override
    public Result ranking() {
        log.warn("远程调用 xfwl-work 失败: ranking()");
        return Result.error("服务降级，利润排行查询失败");
    }
}
