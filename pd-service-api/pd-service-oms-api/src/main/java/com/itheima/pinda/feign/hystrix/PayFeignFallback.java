package com.itheima.pinda.feign.hystrix;

import com.itheima.pinda.common.utils.Result;
import com.itheima.pinda.feign.PayFeign;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 统一支付 Feign 熔断降级
 */
@Slf4j
@Component
public class PayFeignFallback implements PayFeign {

    @Override
    public Result createPayment(String orderId) {
        log.warn("远程调用 pd-oms 支付创建失败: orderId={}", orderId);
        return Result.error(500, "支付服务不可用");
    }

    @Override
    public Result queryPayment(String orderId) {
        log.warn("远程调用 pd-oms 支付查询失败: orderId={}", orderId);
        return Result.error(500, "支付服务不可用");
    }

    @Override
    public Result refund(String orderId) {
        log.warn("远程调用 pd-oms 退款失败: orderId={}", orderId);
        return Result.error(500, "支付服务不可用");
    }
}
