package com.xinfei.config;

import com.xinfei.common.interceptor.SameTokenAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Same-Token 入站鉴权配置（T-SEC-04：堵住绕过网关/BFF 直连服务端口的漏洞）。
 *
 * <p>凭证由网关 ForwardSameTokenFilter 或调用方 Feign 出站拦截器（xfwl-common
 * SameTokenRequestInterceptor）注入；Same-Token 走 Redis 共享变量模式，键前缀 =
 * sa-token.token-name（全端统一 token，教训见 xfwl-dispatch application.yml 注释）。</p>
 *
 * <p>放行清单：/actuator/**（compose HTTP 探活无凭证——《12》§8 部署红线，恒放行）、
 * favicon/error/swagger 文档；其余路径校验失败返回 401（见 SameTokenAuthInterceptor）。</p>
 */
@Configuration
public class SameTokenConfigure implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SameTokenAuthInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns("/actuator/**", "/favicon.ico", "/error",
                        "/swagger-ui/**", "/v3/api-docs/**", "/webjars/**");
    }
}
