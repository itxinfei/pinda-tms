package com.xinfei.feign;

import com.xinfei.common.utils.Result;
import com.xinfei.feign.fallback.ProfitFeignFallback;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 利润查询内部 Feign（P0-6d/FIN-4），对接 xf-work /profit。
 */
@FeignClient(value = "xf-work", fallback = ProfitFeignFallback.class, path = "/profit")
@Hidden
public interface ProfitFeign {

    @GetMapping("/task/{taskId}")
    Result taskProfit(@PathVariable("taskId") Long taskId);

    @GetMapping("/order/{orderId}")
    Result orderProfit(@PathVariable("orderId") Long orderId);

    @GetMapping("/summary")
    Result summary();

    @GetMapping("/ranking")
    Result ranking();
}
