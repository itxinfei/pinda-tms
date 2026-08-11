package com.itheima.pinda.feign;

import com.itheima.pinda.common.utils.Result;
import com.itheima.pinda.feign.hystrix.PayFeignFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import springfox.documentation.annotations.ApiIgnore;

/**
 * 统一支付 Feign 客户端
 *
 * <p>对接 pd-oms 的 /pay 接口（创建支付/查询支付/退款）。</p>
 */
@FeignClient(value = "pd-oms", fallback = PayFeignFallback.class, path = "/pay")
@ApiIgnore
public interface PayFeign {

    /**
     * 创建支付
     *
     * @param orderId 订单ID
     * @return 支付单（含 prepayParams）
     */
    @PostMapping("/create/{orderId}")
    Result createPayment(@PathVariable(name = "orderId") String orderId);

    /**
     * 查询支付状态
     *
     * @param orderId 订单ID
     * @return 支付单
     */
    @GetMapping("/query/{orderId}")
    Result queryPayment(@PathVariable(name = "orderId") String orderId);

    /**
     * 退款
     *
     * @param orderId 订单ID
     * @return 处理结果
     */
    @PostMapping("/refund/{orderId}")
    Result refund(@PathVariable(name = "orderId") String orderId);
}
