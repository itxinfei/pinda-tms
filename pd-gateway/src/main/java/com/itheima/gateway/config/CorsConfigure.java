package com.itheima.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * 全局 CORS：局域网各前端（管理端 / 移动 H5）跨域访问网关统一在此放行。
 * 预检 OPTIONS 请求由本过滤器直接响应，不进入后续鉴权链路。
 */
@Configuration
public class CorsConfigure {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // 用 pattern 而非具体 origin：允许携带凭证（credentials）时 Spring 唯一接受的通配方式
        config.addAllowedOriginPattern("*");
        config.addAllowedMethod(HttpMethod.GET);
        config.addAllowedMethod(HttpMethod.POST);
        config.addAllowedMethod(HttpMethod.PUT);
        config.addAllowedMethod(HttpMethod.DELETE);
        config.addAllowedMethod(HttpMethod.OPTIONS);
        config.addAllowedHeader(HttpHeaders.CONTENT_TYPE);
        config.addAllowedHeader(HttpHeaders.AUTHORIZATION);
        // 全端统一的自定义 token 头
        config.addAllowedHeader("token");
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource(new PathPatternParser());
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }
}
