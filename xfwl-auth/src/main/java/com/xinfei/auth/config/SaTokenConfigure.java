package com.xinfei.auth.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.same.SaSameUtil;
import cn.dev33.satoken.stp.StpUtil;
import com.xinfei.auth.interceptor.OperLogInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 拦截配置。
 * 1) Same-Token：所有请求（含 /anno 匿名接口）必须来自网关——网关转发时一律注入该凭证；
 * 2) 登录校验：/anno/** 匿名放行，其余路径必须登录；
 * 3) 操作日志：/api/v1/** 写请求 + 导出落 OPER 审计（HTTP 健康探针 /actuator/** 恒放行，
 *    无凭证请求不得被任何拦截器拦截——《12》§8 部署红线，否则 compose 探活失败回滚）。
 */
@Configuration
@RequiredArgsConstructor
public class SaTokenConfigure implements WebMvcConfigurer {

    private final OperLogInterceptor operLogInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 网关身份凭证校验：不经排除，/favicon.ico 由浏览器直连场景无需考虑
        registry.addInterceptor(new SaInterceptor(handle -> SaSameUtil.checkCurrentRequestToken()))
                .addPathPatterns("/**")
                .excludePathPatterns("/favicon.ico", "/actuator/**");

        // 登录校验：匿名接口与内部 Feign 端点放行（/internal 仍受上一道 Same-Token 保护，外部无法直达）
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                .addPathPatterns("/**")
                .excludePathPatterns("/anno/**", "/internal/**", "/actuator/**");

        // 写请求审计：网关 StripPrefix=2 后到达本服务的 URI 已剥掉 /api/v1 前缀（实为 /users、/roles 等），
        // 故按 /** 注册并排除匿名/内部 Feign/探针路径；记录 POST/PUT/DELETE 与 GET /logs/export（异常不影响业务）
        registry.addInterceptor(operLogInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/anno/**", "/internal/**", "/actuator/**", "/favicon.ico");
    }
}
