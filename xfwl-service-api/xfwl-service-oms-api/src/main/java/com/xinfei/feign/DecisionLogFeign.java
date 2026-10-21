package com.xinfei.feign;

import com.xinfei.common.utils.Result;
import com.xinfei.feign.fallback.DecisionLogFeignFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 合规决策留痕内部 Feign（P0-9/FR-75），对接 xfwl-oms /decision-log。
 *
 * <p>改派(REASSIGN)埋点待 G2 派单改派落地时经此上报。</p>
 */
@FeignClient(value = "xfwl-oms", fallback = DecisionLogFeignFallback.class, path = "/decision-log")
public interface DecisionLogFeign {

    @PostMapping("/apply")
    Result apply(@RequestParam("businessType") String businessType,
                 @RequestParam("businessId") Long businessId,
                 @RequestParam("reasonEnum") String reasonEnum,
                 @RequestParam(value = "detail", required = false) String detail);

    @GetMapping("/page")
    Result page(@RequestParam(value = "page", required = false) Integer page,
                @RequestParam(value = "pageSize", required = false) Integer pageSize,
                @RequestParam(value = "businessType", required = false) String businessType,
                @RequestParam(value = "businessId", required = false) Long businessId);

    @GetMapping("/export")
    Result export(@RequestParam(value = "businessType", required = false) String businessType,
                  @RequestParam(value = "businessId", required = false) Long businessId,
                  @RequestParam(value = "beginTime", required = false) String beginTime,
                  @RequestParam(value = "endTime", required = false) String endTime);
}
