package com.xinfei.feign.fallback;

import com.xinfei.common.utils.Result;
import com.xinfei.feign.OrderExceptionFeign;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 订单异常（ORD-04）Feign 熔断降级
 */
@Slf4j
@Component
public class OrderExceptionFeignFallback implements OrderExceptionFeign {

    @Override
    public Result register(Long orderId, Integer exceptionType, String description) {
        log.warn("远程调用 xf-oms 失败: register(orderId={})", orderId);
        return Result.error("服务降级，异常登记失败");
    }

    @Override
    public Result handle(Long id, String handleResult) {
        log.warn("远程调用 xf-oms 失败: handle(id={})", id);
        return Result.error("服务降级，异常处理失败");
    }

    @Override
    public Result page(Integer page, Integer pageSize, Long orderId, Integer exceptionType, Integer handleStatus) {
        log.warn("远程调用 xf-oms 失败: page(orderId={})", orderId);
        return Result.error("服务降级，分页查询失败");
    }
}
