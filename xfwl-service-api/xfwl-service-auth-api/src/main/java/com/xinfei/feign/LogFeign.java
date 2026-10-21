package com.xinfei.feign;

import com.xinfei.DTO.AuditLogDTO;
import com.xinfei.feign.fallback.LogFeignFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * 审计日志内部 Feign（SYS-06 跨服务埋点），对接 xfwl-auth /internal/log。
 * 无数据源的服务（xfwl-web-manager 等）写请求经此上报统一落 sys_operation_log；
 * Same-Token 由 xfwl-common SameTokenRequestInterceptor 自动携带。
 */
@FeignClient(value = "xfwl-auth", fallback = LogFeignFallback.class)
public interface LogFeign {

    /**
     * 上报一条审计日志（服务端兜底吞错，不影响调用方业务）。
     */
    @PostMapping("/internal/log")
    void record(@org.springframework.web.bind.annotation.RequestBody AuditLogDTO dto);
}
