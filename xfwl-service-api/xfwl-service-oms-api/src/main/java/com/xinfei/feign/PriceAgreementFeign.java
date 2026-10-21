package com.xinfei.feign;

import com.xinfei.common.utils.Result;
import com.xinfei.feign.fallback.PriceAgreementFeignFallback;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * 客户价格协议（FIN-1.1 / POD-06 费率卡）内部 Feign，对接 xfwl-oms /price-agreement。
 */
@FeignClient(value = "xfwl-oms", fallback = PriceAgreementFeignFallback.class, path = "/price-agreement")
@Hidden
public interface PriceAgreementFeign {

    @PostMapping
    Result save(@RequestBody Map<String, Object> body);

    @PostMapping("/{id}/disable")
    Result disable(@PathVariable("id") Long id);

    @GetMapping("/page")
    Result page(@RequestParam(value = "page", required = false) Integer page,
                @RequestParam(value = "pageSize", required = false) Integer pageSize,
                @RequestParam(value = "memberId", required = false) String memberId,
                @RequestParam(value = "status", required = false) Integer status);
}
