package com.xinfei.feign;

import com.xinfei.common.utils.Result;
import com.xinfei.feign.fallback.FeeItemFeignFallback;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * 附加费费用项配置（POD-03 / FA-10）内部 Feign，对接 xf-oms /fee-item。
 */
@FeignClient(value = "xf-oms", fallback = FeeItemFeignFallback.class, path = "/fee-item")
@Hidden
public interface FeeItemFeign {

    @PostMapping
    Result save(@RequestBody Map<String, Object> body);

    @PostMapping("/{id}/disable")
    Result disable(@PathVariable("id") Long id);

    @GetMapping("/page")
    Result page(@RequestParam(value = "page", required = false) Integer page,
                @RequestParam(value = "pageSize", required = false) Integer pageSize,
                @RequestParam(value = "feeCode", required = false) String feeCode,
                @RequestParam(value = "status", required = false) Integer status);
}
