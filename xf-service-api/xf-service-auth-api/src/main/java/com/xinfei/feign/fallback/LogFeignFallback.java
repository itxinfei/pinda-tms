package com.xinfei.feign.fallback;

import com.xinfei.DTO.AuditLogDTO;
import com.xinfei.feign.LogFeign;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 审计日志 Feign 熔断降级：审计上报失败静默吞掉（只告警不打断业务）。
 */
@Slf4j
@Component
public class LogFeignFallback implements LogFeign {

    @Override
    public void record(AuditLogDTO dto) {
        log.warn("远程调用 xf-auth 失败: LogFeign.record（module={}, uri={}）——审计上报丢弃",
                dto == null ? "?" : dto.getModule(), dto == null ? "?" : dto.getRequestUri());
    }
}
