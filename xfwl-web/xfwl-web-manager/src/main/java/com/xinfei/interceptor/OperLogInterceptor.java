package com.xinfei.interceptor;

import cn.dev33.satoken.stp.StpUtil;
import com.xinfei.DTO.AuditLogDTO;
import com.xinfei.feign.LogFeign;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

/**
 * 写请求操作日志拦截器（SYS-06 跨服务审计埋点）：POST/PUT/DELETE 经 Feign 上报
 * xfwl-auth /internal/log 统一落 sys_operation_log（本服务为纯 BFF 无数据源）。
 * 操作人优先 token 会话账号（manager 有 sa-token），回退网关 userid 头。
 * 上报失败静默吞掉（fallback 只告警），绝不打断业务请求。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OperLogInterceptor implements HandlerInterceptor {

    private static final Map<String, String> ACTION_BY_METHOD =
            Map.of("POST", "新增", "PUT", "修改", "DELETE", "删除");

    private final LogFeign logFeign;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        request.setAttribute(OperLogInterceptor.class.getName() + ".start", System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response,
                                @NonNull Object handler, Exception ex) {
        try {
            String method = request.getMethod().toUpperCase();
            String uri = request.getRequestURI();
            if (!ACTION_BY_METHOD.containsKey(method) || uri.startsWith("/internal/")
                    || uri.startsWith("/actuator/") || uri.startsWith("/error")) {
                return;
            }

            Long start = (Long) request.getAttribute(OperLogInterceptor.class.getName() + ".start");
            AuditLogDTO dto = new AuditLogDTO();
            dto.setLogType("OPER");
            dto.setModule("manager");
            dto.setActionType(ACTION_BY_METHOD.get(method));
            dto.setTitle("manager·" + dto.getActionType());
            dto.setRequestUri(uri);
            dto.setRequestMethod(method);
            dto.setIp(clientIp(request));
            fillOperator(dto, request);
            boolean failed = ex != null || response.getStatus() >= 400;
            dto.setStatus(failed ? 0 : 1);
            dto.setExecutionTime(start == null ? null
                    : (int) Math.min(Integer.MAX_VALUE, System.currentTimeMillis() - start));
            dto.setErrorMsg(ex != null && ex.getMessage() != null
                    ? ex.getMessage().substring(0, Math.min(500, ex.getMessage().length()))
                    : "");
            logFeign.record(dto);
        } catch (Exception e) {
            log.warn("审计上报失败（不影响业务）: {}", e.getMessage());
        }
    }

    /** 操作人：优先 Sa-Token 会话账号，回退网关 userid 头 */
    private void fillOperator(AuditLogDTO dto, HttpServletRequest request) {
        try {
            if (StpUtil.isLogin()) {
                dto.setOperatorId(StpUtil.getLoginIdAsLong());
                Object account = StpUtil.getTokenSession().get("account");
                if (account != null) {
                    dto.setOperatorName(String.valueOf(account));
                    return;
                }
            }
        } catch (Exception ignore) {
            // 无会话上下文
        }
        dto.setOperatorId(parseLong(request.getHeader("userid")));
        dto.setOperatorName("");
    }

    private Long parseLong(String s) {
        if (!StringUtils.hasText(s)) {
            return null;
        }
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String clientIp(HttpServletRequest request) {
        String[] headers = {"X-Real-IP", "X-Forwarded-For", "Proxy-Client-IP"};
        for (String header : headers) {
            String value = request.getHeader(header);
            if (StringUtils.hasText(value) && !"unknown".equalsIgnoreCase(value)) {
                int comma = value.indexOf(',');
                return comma > 0 ? value.substring(0, comma).trim() : value.trim();
            }
        }
        return request.getRemoteAddr();
    }
}
