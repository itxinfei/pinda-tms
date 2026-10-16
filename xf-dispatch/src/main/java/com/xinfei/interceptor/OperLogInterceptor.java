package com.xinfei.interceptor;

import com.xinfei.entity.SysOperationLog;
import com.xinfei.mapper.SysOperationLogMapper;
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
 * 写请求操作日志拦截器（SYS-06 跨服务审计埋点）：对 POST/PUT/DELETE 自动落 OPER 日志。
 * 无 sa-token 依赖，操作人取网关注入的 userid 头（D-30 关键动作：调度异常处理等留痕）。
 * 落库失败绝不打断业务请求；查询归 xf-auth 统一出口。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OperLogInterceptor implements HandlerInterceptor {

    private static final Map<String, String> ACTION_BY_METHOD =
            Map.of("POST", "新增", "PUT", "修改", "DELETE", "删除");

    private final SysOperationLogMapper logMapper;

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
                    || uri.startsWith("/actuator/")) {
                return;
            }

            Long start = (Long) request.getAttribute(OperLogInterceptor.class.getName() + ".start");
            SysOperationLog row = new SysOperationLog();
            row.setLogType(SysOperationLog.TYPE_OPER);
            row.setModule("dispatch");
            row.setActionType(ACTION_BY_METHOD.get(method));
            row.setTitle("dispatch·" + row.getActionType());
            row.setRequestUri(uri);
            row.setRequestMethod(method);
            row.setIp(clientIp(request));
            row.setOperatorId(parseLong(request.getHeader("userid")));
            row.setOperatorName("");
            boolean failed = ex != null || response.getStatus() >= 400;
            row.setStatus(failed ? 0 : 1);
            row.setErrorMsg(ex != null && ex.getMessage() != null
                    ? ex.getMessage().substring(0, Math.min(500, ex.getMessage().length()))
                    : "");
            row.setExecutionTime(start == null ? null
                    : (int) Math.min(Integer.MAX_VALUE, System.currentTimeMillis() - start));
            if (row.getCreateTime() == null) {
                row.setCreateTime(java.time.LocalDateTime.now());
            }
            logMapper.insert(row);
        } catch (Exception e) {
            log.warn("操作日志落库失败（不影响业务）: {}", e.getMessage());
        }
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
