package com.xinfei.auth.interceptor;

import cn.dev33.satoken.stp.StpUtil;
import com.xinfei.auth.common.ClientIps;
import com.xinfei.auth.entity.SysOperationLog;
import com.xinfei.auth.service.v1.LogService;
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
 * 写请求操作日志拦截器：对本服务管理端点的 POST/PUT/DELETE 自动落 OPER 日志
 * （SYS-06 审计口径；业务写操作落点在 web-manager 等服务，本拦截器只覆盖 xfwl-auth 自身管理端点）。
 *
 * <p>路径口径：外部经网关访问 /api/v1/users 时 StripPrefix=2 会剥掉 /api/v1，
 * 到达本服务的 requestURI 实为 /users；旧契约 /api/auth/org 同理剥成 /org。
 * 故本拦截器在 SaTokenConfigure 中按 /** 注册（排除 /anno、/internal、/actuator），
 * 这里不得再以前缀 /api/v1/ 判定（2026-10-22 修复：旧写法导致系统管理写操作零审计）。</p>
 *
 * <p>导出（GET /logs/export）虽是 GET 也需留痕，由 path 白名单单独放行。
 * 落库异常绝不打断业务请求。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OperLogInterceptor implements HandlerInterceptor {

    /** GET 白名单：虽非写方法但属关键审计动作（URI 为网关剥前缀后的服务内路径） */
    private static final String[] GET_AUDIT_PATHS = {"/logs/export"};

    private static final Map<String, String> ACTION_BY_METHOD =
            Map.of("POST", "新增", "PUT", "修改", "DELETE", "删除");

    private final LogService logService;

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
            String method = request.getMethod();
            String uri = request.getRequestURI();
            boolean audited = "GET".equalsIgnoreCase(method)
                    ? startsWithAny(uri, GET_AUDIT_PATHS)
                    : ACTION_BY_METHOD.containsKey(method.toUpperCase());
            // 注册层已排除 /anno、/internal、/actuator；此处不再做 /api/v1 前缀判断
            // （网关已剥前缀，服务内 URI 为 /users、/org 等，加前缀判断会导致审计全部漏记）
            if (!audited) {
                return;
            }

            Long start = (Long) request.getAttribute(OperLogInterceptor.class.getName() + ".start");
            SysOperationLog row = new SysOperationLog();
            row.setLogType(SysOperationLog.TYPE_OPER);
            row.setModule(moduleOf(uri));
            row.setActionType(ACTION_BY_METHOD.getOrDefault(method.toUpperCase(), "导出"));
            row.setTitle(row.getModule() + "·" + row.getActionType());
            row.setRequestUri(uri);
            row.setRequestMethod(method.toUpperCase());
            row.setIp(ClientIps.resolve(request));
            fillUserAgent(row, request.getHeader("User-Agent"));
            fillOperator(row);
            boolean failed = ex != null || response.getStatus() >= 400;
            row.setStatus(failed ? 0 : 1);
            row.setErrorMsg(truncate(ex != null ? ex.getMessage() : ""));
            row.setExecutionTime(start == null ? null
                    : (int) Math.min(Integer.MAX_VALUE, System.currentTimeMillis() - start));
            logService.record(row);
        } catch (Exception e) {
            log.warn("操作日志落库失败（不影响业务）: {}", e.getMessage());
        }
    }

    /** 操作人取当前登录态；登录前请求（理论不存在，防御）置空 */
    private void fillOperator(SysOperationLog row) {
        try {
            if (StpUtil.isLogin()) {
                row.setOperatorId(StpUtil.getLoginIdAsLong());
                Object account = StpUtil.getTokenSession().get("account");
                if (account != null) {
                    row.setOperatorName(String.valueOf(account));
                }
            }
        } catch (Exception ignore) {
            // 无会话上下文，留空
        }
    }

    private void fillUserAgent(SysOperationLog row, String ua) {
        if (!StringUtils.hasText(ua)) {
            return;
        }
        String lower = ua.toLowerCase();
        if (lower.contains("mobile") || lower.contains("android") || lower.contains("iphone")) {
            row.setDevice("Mobile");
        } else if (lower.contains("windows") || lower.contains("macintosh") || lower.contains("linux")) {
            row.setDevice("PC");
        }
        if (lower.contains("edg/")) {
            row.setBrowser("Edge");
        } else if (lower.contains("chrome/") && !lower.contains("edg/")) {
            row.setBrowser("Chrome");
        } else if (lower.contains("firefox/")) {
            row.setBrowser("Firefox");
        } else if (lower.contains("safari/") && !lower.contains("chrome")) {
            row.setBrowser("Safari");
        }
        if (lower.contains("windows")) {
            row.setOs("Windows");
        } else if (lower.contains("mac os")) {
            row.setOs("macOS");
        } else if (lower.contains("android")) {
            row.setOs("Android");
        } else if (lower.contains("iphone") || lower.contains("ipad")) {
            row.setOs("iOS");
        }
    }

    /**
     * 模块名取服务内 URI 首段，如 /users → users、/tenant-plans/{id}/menus → tenant-plans。
     * 网关已剥 /api/v1、/api/auth 前缀，不能再按固定下标取段。
     */
    private String moduleOf(String uri) {
        String[] parts = uri.split("/");
        return parts.length > 1 ? parts[1] : uri;
    }

    private boolean startsWithAny(String uri, String[] prefixes) {
        for (String p : prefixes) {
            if (uri.startsWith(p)) {
                return true;
            }
        }
        return false;
    }

    private String truncate(String s) {
        if (!StringUtils.hasText(s)) {
            return "";
        }
        return s.length() > 500 ? s.substring(0, 500) : s;
    }
}
