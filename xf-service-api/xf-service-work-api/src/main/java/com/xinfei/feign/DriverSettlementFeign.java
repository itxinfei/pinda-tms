package com.xinfei.feign;

import com.xinfei.common.utils.Result;
import com.xinfei.feign.fallback.DriverSettlementFeignFallback;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

/**
 * 司机趟次结算内部 Feign（FIN-2），对接 xf-work /driver-settlement。
 * 账期走 String（yyyy-MM-dd），由 xf-work 绑 LocalDate。
 */
@FeignClient(value = "xf-work", fallback = DriverSettlementFeignFallback.class, path = "/driver-settlement")
@Hidden
public interface DriverSettlementFeign {

    @PostMapping("")
    Result generate(@RequestParam("driverId") Long driverId,
                    @RequestParam("periodStart") String periodStart,
                    @RequestParam("periodEnd") String periodEnd,
                    @RequestParam(value = "baseWage", required = false) BigDecimal baseWage,
                    @RequestParam(value = "allowance", required = false) BigDecimal allowance,
                    @RequestParam(value = "deduction", required = false) BigDecimal deduction,
                    @RequestParam(value = "remark", required = false) String remark);

    @GetMapping("/drivers")
    Result drivers(@RequestParam("periodStart") String periodStart,
                   @RequestParam("periodEnd") String periodEnd);

    @PostMapping("/page")
    Result page(@RequestParam(value = "page", required = false) Integer page,
                @RequestParam(value = "pageSize", required = false) Integer pageSize,
                @RequestParam(value = "settleNo", required = false) String settleNo,
                @RequestParam(value = "driverId", required = false) Long driverId,
                @RequestParam(value = "status", required = false) Integer status,
                @RequestParam(value = "periodStart", required = false) String periodStart,
                @RequestParam(value = "periodEnd", required = false) String periodEnd);

    @GetMapping("/{id}")
    Result detail(@PathVariable("id") Long id);

    @PostMapping("/{id}/audit")
    Result audit(@PathVariable("id") Long id,
                 @RequestParam(value = "auditRemark", required = false) String auditRemark);

    @PostMapping("/{id}/pay")
    Result pay(@PathVariable("id") Long id);
}
