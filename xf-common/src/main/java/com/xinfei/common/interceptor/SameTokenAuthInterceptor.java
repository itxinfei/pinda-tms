package com.xinfei.common.interceptor;

import cn.dev33.satoken.same.SaSameUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.UUID;

/**
 * 服务间 Same-Token 入站鉴权拦截器（T-SEC-04：堵住绕过网关/BFF 直连服务端口的漏洞）
 *
 * <p>凭证来源：网关（xf-gateway）的 ForwardSameTokenFilter 对全部下游请求无条件注入
 * {@link SaSameUtil#SAME_TOKEN} 请求头；服务间 Feign 调用由
 * {@code com.xinfei.common.feign.SameTokenRequestInterceptor} 出站注入。</p>
 *
 * <p>校验口径与 xf-auth SaTokenConfigure 一致（Same-Token 未配 secret-key 时走 Redis
 * 共享变量模式）：各服务引入 sa-token starter + sa-token-redis-jackson 并连接同一 Redis
 * 即可校验，键前缀 = sa-token.token-name，全端必须统一为 token（默认值 satoken 会使
 * 写读键不一致恒 401，教训见 xf-dispatch application.yml 注释）。</p>
 *
 * <p>非 Spring 组件（显式注册模式，同 {@link TokenAuthInterceptor}）：sa-token-core
 * 在 xf-common 中为 optional 依赖，仅由引了 sa-token 依赖的服务在注册类中 new 出实例，
 * 避免无依赖应用组件扫描时类加载失败。放行清单由注册处 excludePathPatterns 声明，
 * {@code /actuator/**} 必须恒放行（compose HTTP 探活无凭证——《12》§8 部署红线）。</p>
 */
@Slf4j
public class SameTokenAuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 链路追踪（T-SEC-01/A2）：网关生成的 X-Trace-Id 落入 MDC，logback pattern 按 %X{traceId} 输出；
        // 缺失时兜底生成（直连场景），保证每条日志可归属一条请求链
        String traceId = request.getHeader("X-Trace-Id");
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        MDC.put("traceId", traceId);
        try {
            // 非控制器方法（静态资源等）直接放行
            if (!(handler instanceof HandlerMethod)) {
                return true;
            }
            try {
                // 头缺失/无效/Redis 异常均抛异常 → 统一按 401 拒绝（fail-close）
                SaSameUtil.checkToken(request.getHeader(SaSameUtil.SAME_TOKEN));
                return true;
            } catch (Exception e) {
                log.warn("[鉴权] 请求缺少/携带无效 Same-Token，拒绝访问: uri={}, remote={}, reason={}",
                    request.getRequestURI(), request.getRemoteAddr(), e.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":401,\"msg\":\"缺少有效的 Same-Token，请通过网关访问\"}");
                // preHandle 返回 false 时 afterCompletion 不会被调用，这里必须手动清 MDC 防线程复用串号
                MDC.remove("traceId");
                return false;
            }
        } catch (Exception e) {
            MDC.remove("traceId");
            throw e;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 请求结束清 MDC，防线程池复用把上一条请求的 traceId 带进无关日志
        MDC.remove("traceId");
    }
}
