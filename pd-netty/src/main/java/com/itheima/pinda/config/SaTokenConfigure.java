package com.itheima.pinda.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.same.SaSameUtil;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 拦截配置（2026-10-11 防绕网关直连加固）：
 * <ol>
 *   <li>Same-Token：所有 HTTP 请求必须来自网关——网关转发时统一注入该凭证，
 *       堵住绕过网关直连 8192 端口访问轨迹/告警接口；</li>
 *   <li>登录校验：pd-netty 的 HTTP 端点（轨迹查询/回放、告警查询处理、HTTP 上报）均为登录后前端调用，全部要求登录。</li>
 * </ol>
 * 注意：TCP 设备上报（NettyServer 8194 长连接）与 RabbitMQ 消费不经过 Spring MVC，不受本拦截器影响；
 * 服务间无入站 Feign/HTTP 调用 pd-netty（pd-netty 只出站调 pd-base），故无需 /internal 放行。
 */
@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 网关身份凭证校验：不经业务排除（与 pd-auth 同一姿势）
        registry.addInterceptor(new SaInterceptor(handle -> SaSameUtil.checkCurrentRequestToken()))
                .addPathPatterns("/**")
                .excludePathPatterns("/favicon.ico");

        // 登录校验：pd-netty 无匿名 HTTP 端点，全部要求登录
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                .addPathPatterns("/**");
    }
}
