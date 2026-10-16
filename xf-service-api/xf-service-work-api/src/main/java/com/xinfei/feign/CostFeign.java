package com.xinfei.feign;

import com.xinfei.common.utils.Result;
import com.xinfei.feign.fallback.CostFeignFallback;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 趟次成本内部 Feign（P0-6d/OPS-4），对接 xf-work /cost。
 * occurTime 走 String（yyyy-MM-dd HH:mm:ss），由 xf-work 绑 LocalDateTime。
 */
@FeignClient(value = "xf-work", fallback = CostFeignFallback.class, path = "/cost")
@Hidden
public interface CostFeign {

    @PostMapping("")
    Result save(@RequestParam("taskTransportId") Long taskTransportId,
                @RequestParam(value = "truckId", required = false) Long truckId,
                @RequestParam(value = "driverId", required = false) Long driverId,
                @RequestParam("costType") String costType,
                @RequestParam("amount") java.math.BigDecimal amount,
                @RequestParam("occurTime") String occurTime,
                @RequestParam(value = "voucher", required = false) String voucher,
                @RequestParam(value = "remark", required = false) String remark);

    @GetMapping("/task/{taskId}")
    Result taskDetail(@PathVariable("taskId") Long taskId);

    @PostMapping("/page")
    Result page(@RequestParam(value = "page", required = false) Integer page,
                @RequestParam(value = "pageSize", required = false) Integer pageSize,
                @RequestParam(value = "taskTransportId", required = false) Long taskTransportId,
                @RequestParam(value = "truckId", required = false) Long truckId,
                @RequestParam(value = "costType", required = false) String costType);
}
