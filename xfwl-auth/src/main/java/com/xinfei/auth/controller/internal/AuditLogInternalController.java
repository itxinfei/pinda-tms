package com.xinfei.auth.controller.internal;

import com.xinfei.DTO.AuditLogDTO;
import com.xinfei.auth.entity.SysOperationLog;
import com.xinfei.auth.service.v1.LogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 审计日志内部上报（SYS-06 跨服务埋点）：供无数据源的服务（如 xfwl-web-manager BFF）
 * 经 Feign 上报写操作审计，由 xfwl-auth 统一落 sys_operation_log。
 * Same-Token 由 xfwl-common 拦截器校验（仅网关/内网服务可达）。
 */
@Slf4j
@RestController
@RequestMapping("/internal/log")
@RequiredArgsConstructor
public class AuditLogInternalController {

    private final LogService logService;

    @PostMapping
    public void record(@RequestBody AuditLogDTO dto) {
        try {
            SysOperationLog row = new SysOperationLog();
            row.setLogType(StringUtils.hasText(dto.getLogType()) ? dto.getLogType() : SysOperationLog.TYPE_OPER);
            row.setModule(dto.getModule());
            row.setActionType(dto.getActionType());
            row.setTitle(dto.getTitle());
            row.setRequestUri(dto.getRequestUri());
            row.setRequestMethod(dto.getRequestMethod());
            row.setIp(dto.getIp());
            row.setOperatorId(dto.getOperatorId());
            row.setOperatorName(dto.getOperatorName() == null ? "" : dto.getOperatorName());
            row.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
            row.setExecutionTime(dto.getExecutionTime());
            row.setErrorMsg(dto.getErrorMsg() == null ? "" : dto.getErrorMsg());
            logService.record(row);
        } catch (Exception e) {
            // 审计上报失败只记日志，绝不影响调用方业务
            log.warn("审计日志上报落库失败: {}", e.getMessage());
        }
    }
}
