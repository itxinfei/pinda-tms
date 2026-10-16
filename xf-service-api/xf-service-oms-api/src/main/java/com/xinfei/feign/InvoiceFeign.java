package com.xinfei.feign;

import com.xinfei.common.utils.Result;
import com.xinfei.feign.fallback.InvoiceFeignFallback;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 电子发票（开票）内部 Feign（P0-6c），对接 xf-oms /invoice。
 */
@FeignClient(value = "xf-oms", fallback = InvoiceFeignFallback.class, path = "/invoice")
@Hidden
public interface InvoiceFeign {

    @PostMapping("/apply")
    Result apply(@RequestParam("billId") Long billId,
                 @RequestParam(value = "type", required = false) Integer type,
                 @RequestParam(value = "taxNo", required = false) String taxNo,
                 @RequestParam(value = "mailAddress", required = false) String mailAddress);

    @PostMapping("/{id}/issue")
    Result issue(@PathVariable("id") Long id);

    @PostMapping("/{id}/mail")
    Result mail(@PathVariable("id") Long id);

    @PostMapping("/{id}/void")
    Result voidInvoice(@PathVariable("id") Long id, @RequestParam("reason") String reason);

    @GetMapping("/page")
    Result page(@RequestParam(value = "page", required = false) Integer page,
                @RequestParam(value = "pageSize", required = false) Integer pageSize,
                @RequestParam(value = "customerId", required = false) String customerId,
                @RequestParam(value = "status", required = false) Integer status);
}
