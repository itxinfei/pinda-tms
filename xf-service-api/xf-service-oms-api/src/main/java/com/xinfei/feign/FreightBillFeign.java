package com.xinfei.feign;

import com.xinfei.common.utils.Result;
import com.xinfei.feign.fallback.FreightBillFeignFallback;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 运费账单（对账）内部 Feign（P0-6b），对接 xf-oms /freight-bill。
 *
 * <p>账期参数走 String（避免 LocalDate 跨 Feign 序列化歧义），由 xf-oms 侧 Spring 绑定为 LocalDate。</p>
 */
@FeignClient(value = "xf-oms", fallback = FreightBillFeignFallback.class, path = "/freight-bill")
@Hidden
public interface FreightBillFeign {

    @PostMapping("/generate")
    Result generate(@RequestParam("customerId") String customerId,
                    @RequestParam("periodStart") String periodStart,
                    @RequestParam("periodEnd") String periodEnd);

    @GetMapping("/page")
    Result page(@RequestParam(value = "page", required = false) Integer page,
                @RequestParam(value = "pageSize", required = false) Integer pageSize,
                @RequestParam(value = "billNo", required = false) String billNo,
                @RequestParam(value = "customerId", required = false) String customerId,
                @RequestParam(value = "status", required = false) Integer status);

    @GetMapping("/{id}")
    Result reconcile(@PathVariable("id") Long id);

    @PostMapping("/{id}/confirm")
    Result confirm(@PathVariable("id") Long id);

    @PostMapping("/{id}/write-off")
    Result writeOff(@PathVariable("id") Long id);
}
