package com.xinfei.feign.fallback;

import com.xinfei.common.utils.Result;
import com.xinfei.feign.OrderFeeFeign;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 订单附加费明细 Feign 熔断降级
 */
@Slf4j
@Component
public class OrderFeeFeignFallback implements OrderFeeFeign {

    @Override
    public Result list(String orderId) {
        log.warn("远程调用 xf-oms 失败: order-fee.list(orderId={})", orderId);
        return Result.error("服务降级，附加费明细查询失败");
    }

    @Override
    public Result page(String orderId, Integer page, Integer pageSize) {
        log.warn("远程调用 xf-oms 失败: order-fee.page(orderId={})", orderId);
        return Result.error("服务降级，附加费查询失败");
    }

    @Override
    public Result addManual(Map<String, Object> body) {
        log.warn("远程调用 xf-oms 失败: order-fee.addManual");
        return Result.error("服务降级，附加费补录失败");
    }

    @Override
    public Result calculate(Long orderId) {
        log.warn("远程调用 xf-oms 失败: order-fee.calculate(orderId={})", orderId);
        return Result.error("服务降级，附加费生成失败");
    }
}
