package com.xinfei.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.List;

/**
 * 全局 CORS（T-SEC-02/A4 收敛）：跨域访问网关统一在此放行。
 * 预检 OPTIONS 请求由本过滤器直接响应，不进入后续鉴权链路。
 *
 * <p>origin 白名单经配置注入（{@code xinfei.security.cors.allowed-origin-patterns}，
 * 逗号分隔 pattern，Nacos 可覆盖）：生产必须收敛为管理端 + 三端 H5 的域名白名单；
 * 未配置（dev 内网联调）回落通配 {@code *}，与《71》T-SEC-02 "dev 保留通配" 口径一致。</p>
 */
@Configuration
public class CorsConfigure {

    /** 允许的 origin pattern 白名单（如 https://admin.xinfei.cn,https://*.xinfei.cn） */
    @Value("${xinfei.security.cors.allowed-origin-patterns:}")
    private List<String> allowedOriginPatterns;

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // 用 pattern 而非具体 origin：允许携带凭证（credentials）时 Spring 唯一接受的通配方式
        if (allowedOriginPatterns == null || allowedOriginPatterns.isEmpty()) {
            // 未配置白名单（dev 内网联调）：通配；生产必须经 Nacos 覆盖为显式域名清单
            config.addAllowedOriginPattern("*");
        } else {
            allowedOriginPatterns.stream()
                    .filter(o -> o != null && !o.isBlank())
                    .map(String::trim)
                    .forEach(config::addAllowedOriginPattern);
        }
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
