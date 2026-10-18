package com.xinfei.config;

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
 *   <li>登录校验：xf-netty 的 HTTP 端点（轨迹查询/回放、告警查询处理、HTTP 上报）均为登录后前端调用，全部要求登录。</li>
 * </ol>
 * 注意：TCP 设备上报（NettyServer 8194 长连接）与 RabbitMQ 消费不经过 Spring MVC，不受本拦截器影响。
 *
 * <p>服务间例外（2026-10-18）：BFF 经 Feign 调用 xf-netty 时只携带 Same-Token（无登录 token），
 * 需将对应端点排除登录校验，否则第二道拦截器必 401：
 * <ul>
 *   <li>{@code /alarm/report}：xf-web-courier（D-41）/ xf-web-driver（D-57）异常上报转发；</li>
 *   <li>{@code /trace/byTasks}：xf-web-customer（凭订单查在途轨迹）转发。</li>
 * </ul>
 * 安全性不变：外部流量仍受网关登录闸门约束，且第一道 Same-Token 校验对上述路径照常生效（防绕网关直连）。</p>
 */
@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 网关身份凭证校验：不经业务排除（与 xf-auth 同一姿势）
        // /actuator/** 放行：compose HTTP 探针不带 Same-Token，纳入校验必 401
        // （run#297 auto-deploy 网关修后轮到 netty 探活失败全量回滚的根因，2026-10-14）
        registry.addInterceptor(new SaInterceptor(handle -> SaSameUtil.checkCurrentRequestToken()))
                .addPathPatterns("/**")
                .excludePathPatterns("/favicon.ico", "/actuator/**");

        // 登录校验：xf-netty 无匿名 HTTP 端点，默认全部要求登录；
        // 例外为服务间 Feign 转发端点（见类注释），不携带登录 token。
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                .addPathPatterns("/**")
                .excludePathPatterns("/actuator/**", "/alarm/report", "/trace/byTasks");
    }
}
