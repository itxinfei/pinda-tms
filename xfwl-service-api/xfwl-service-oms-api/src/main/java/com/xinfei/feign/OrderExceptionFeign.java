package com.xinfei.feign;

import com.xinfei.common.utils.Result;
import com.xinfei.feign.fallback.OrderExceptionFeignFallback;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 订单异常（ORD-04）内部 Feign，对接 xfwl-oms /order-exception。
 */
@FeignClient(value = "xfwl-oms", fallback = OrderExceptionFeignFallback.class, path = "/order-exception")
@Hidden
public interface OrderExceptionFeign {

    @PostMapping("/register")
    Result register(@RequestParam("orderId") Long orderId,
                    @RequestParam("exceptionType") Integer exceptionType,
                    @RequestParam("description") String description);

    @PostMapping("/{id}/handle")
    Result handle(@PathVariable("id") Long id,
                  @RequestParam("handleResult") String handleResult);

    @GetMapping("/page")
    Result page(@RequestParam(value = "page", required = false) Integer page,
                @RequestParam(value = "pageSize", required = false) Integer pageSize,
                @RequestParam(value = "orderId", required = false) Long orderId,
                @RequestParam(value = "exceptionType", required = false) Integer exceptionType,
                @RequestParam(value = "handleStatus", required = false) Integer handleStatus);
}
