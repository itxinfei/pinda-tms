package com.xinfei.config;

import cn.dev33.satoken.same.SaSameUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Same-Token 入站鉴权配置（T-SEC-04：堵住绕过网关/BFF 直连服务端口的漏洞）。
 *
 * <p>本服务有意不依赖 xfwl-common（避免拖入数据源/MyBatis 栈），故在此内联极简校验器，
 * 校验口径与 xfwl-auth SaTokenConfigure 一致：Same-Token 未配 secret-key 时走 Redis
 * 共享变量模式，键前缀 = sa-token.token-name（全端统一 token，教训见 xfwl-dispatch
 * application.yml 注释）。</p>
 *
 * <p>放行 /actuator/**（compose HTTP 探活无凭证——《12》§8 部署红线，恒放行）；
 * 匿名查看路由 /view/** 经网关转发时由 ForwardSameTokenFilter 无条件注入 Same-Token，
 * 无需放行。</p>
 */
@Configuration
public class SameTokenConfigure implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(SameTokenConfigure.class);

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
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
                    return false;
                }
            }
        }).addPathPatterns("/**")
          .excludePathPatterns("/actuator/**", "/favicon.ico", "/error");
    }
}
