package com.itheima.pinda.common.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Web 端 Token 鉴权拦截器（轻量，依赖网关透传身份）
 *
 * <p>网关（pd-gateway）在校验 JWT 后将用户ID写入 {@code userid} 请求头并向下游透传。
 * 本拦截器作为服务端兜底校验：业务接口（HandlerMethod）缺少 userid 头时直接拒绝，
 * 防止绕过网关直连端口访问。静态资源与 Swagger 文档放行。</p>
 */
@Slf4j
public class TokenAuthInterceptor implements HandlerInterceptor {

    /**
     * 网关透传的用户ID请求头
     */
    private static final String USER_ID_HEADER = "userid";

    /**
     * 放行前缀：Swagger/静态资源/健康检查等
     */
    private static final String[] EXCLUDE_PREFIXES = {
        "/swagger", "/v2/api-docs", "/webjars", "/favicon.ico",
        "/error", "/actuator", "/csrf"
    };

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 非控制器方法（静态资源等）直接放行
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        String uri = request.getRequestURI();
        for (String prefix : EXCLUDE_PREFIXES) {
            if (uri.startsWith(prefix)) {
                return true;
            }
        }
        // 校验网关透传的身份头
        String userId = request.getHeader(USER_ID_HEADER);
        if (StringUtils.isBlank(userId)) {
            log.warn("[鉴权] 请求缺少用户身份(userid头)，拒绝访问: uri={}, remote={}",
                uri, request.getRemoteAddr());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"未登录或身份信息缺失，请通过网关访问\"}");
            return false;
        }
        return true;
    }
}
