package com.xinfei.feign.carrier;

import com.xinfei.common.utils.Result;
import com.xinfei.feign.carrier.fallback.CarrierFeignFallback;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

/**
 * 承运商/外协运力 Feign（DSP-2 第一期）
 */
@FeignClient(value = "xf-base", fallback = CarrierFeignFallback.class, path = "/carrier")
@Hidden
public interface CarrierFeign {

    @PostMapping("")
    Result save(@RequestBody java.util.Map<String, Object> body);

    @GetMapping("/page")
    Result page(@RequestParam(value = "page", required = false) Integer page,
                @RequestParam(value = "pageSize", required = false) Integer pageSize,
                @RequestParam(value = "code", required = false) String code,
                @RequestParam(value = "name", required = false) String name,
                @RequestParam(value = "status", required = false) Integer status);

    @GetMapping("/{id}")
    Result detail(@PathVariable("id") Long id);

    @PostMapping("/{id}/status")
    Result changeStatus(@PathVariable("id") Long id, @RequestParam("status") Integer status);

    @PostMapping("/truck")
    Result saveTruck(@RequestBody java.util.Map<String, Object> body);

    @PostMapping("/driver")
    Result saveDriver(@RequestBody java.util.Map<String, Object> body);

    @GetMapping("/capacity")
    Result capacity(@RequestParam(value = "keyword", required = false) String keyword,
                    @RequestParam(value = "truckTypeId", required = false) Long truckTypeId,
                    @RequestParam(value = "minLoad", required = false) BigDecimal minLoad);
}
