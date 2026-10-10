package com.itheima.auth.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.same.SaSameUtil;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 拦截配置。
 * 1) Same-Token：所有请求（含 /anno 匿名接口）必须来自网关——网关转发时一律注入该凭证；
 * 2) 登录校验：/anno/** 匿名放行，其余路径必须登录。
 */
@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 网关身份凭证校验：不经排除，/favicon.ico 由浏览器直连场景无需考虑
        registry.addInterceptor(new SaInterceptor(handle -> SaSameUtil.checkCurrentRequestToken()))
                .addPathPatterns("/**")
                .excludePathPatterns("/favicon.ico");

        // 登录校验：匿名接口与内部 Feign 端点放行（/internal 仍受上一道 Same-Token 保护，外部无法直达）
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                .addPathPatterns("/**")
                .excludePathPatterns("/anno/**", "/internal/**");
    }
}
