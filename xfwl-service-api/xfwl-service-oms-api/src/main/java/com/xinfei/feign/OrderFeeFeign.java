package com.xinfei.feign;

import com.xinfei.common.utils.Result;
import com.xinfei.feign.fallback.OrderFeeFeignFallback;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * 订单附加费明细（POD-03 第一期）内部 Feign，对接 xfwl-oms /order-fee。
 */
@FeignClient(value = "xfwl-oms", fallback = OrderFeeFeignFallback.class, path = "/order-fee")
@Hidden
public interface OrderFeeFeign {

    @GetMapping("/list")
    Result list(@RequestParam("orderId") String orderId);

    @GetMapping("/page")
    Result page(@RequestParam("orderId") String orderId,
                @RequestParam(value = "page", required = false) Integer page,
                @RequestParam(value = "pageSize", required = false) Integer pageSize);

    @PostMapping
    Result addManual(@RequestBody Map<String, Object> body);

    @PostMapping("/calculate/{orderId}")
    Result calculate(@PathVariable("orderId") Long orderId);
}
