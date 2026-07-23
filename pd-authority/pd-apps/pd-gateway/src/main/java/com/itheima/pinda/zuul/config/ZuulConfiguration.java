package com.itheima.pinda.zuul.config;

import com.itheima.pinda.common.config.BaseConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

/**
 * 解决跨域问题
 * <p>
 * 安全警告：allowCredentials=true 时不允许使用通配符 Origin，
 * 必须指定具体的允许域名列表，否则存在 CSRF 攻击风险。
 * </p>
 */
@Configuration
public class ZuulConfiguration extends BaseConfig {
    /**
     * 允许跨域的域名白名单（生产环境必须配置实际域名）
     * 可通过系统属性 -Dcors.allowed.origins=https://xxx.com,https://yyy.com 覆盖
     */
    private static final List<String> ALLOWED_ORIGINS = Arrays.asList(
            System.getProperty("cors.allowed.origins", "http://localhost:8080,http://localhost:3000").split(",")
    );

    @Bean
    public CorsFilter corsFilter() {
        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        final org.springframework.web.cors.CorsConfiguration config = new org.springframework.web.cors.CorsConfiguration();
        // 允许cookies跨域
        config.setAllowCredentials(true);
        // 只允许白名单中的域名
        for (String origin : ALLOWED_ORIGINS) {
            config.addAllowedOrigin(origin.trim());
        }
        // 允许访问的头信息,*表示全部
        config.addAllowedHeader("*");
        // 预检请求的缓存时间（秒）
        config.setMaxAge(18000L);
        // 允许提交请求的方法
        config.addAllowedMethod("OPTIONS");
        config.addAllowedMethod("HEAD");
        config.addAllowedMethod("GET");
        config.addAllowedMethod("PUT");
        config.addAllowedMethod("POST");
        config.addAllowedMethod("DELETE");
        config.addAllowedMethod("PATCH");
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
