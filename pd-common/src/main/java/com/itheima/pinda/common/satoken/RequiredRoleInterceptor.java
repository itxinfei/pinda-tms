package com.itheima.pinda.common.satoken;

import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import java.util.Arrays;

/**
 * 端级角色绑定拦截器（X-01 下沉落点，《12》§8.5）。
 *
 * <p>角色校验必须在<b>持有角色数据的下游</b>做：角色来自 pd-auth 登录时写入
 * token-session 的 {@code roles}（同一 Redis），本拦截器经
 * {@link SharedSessionStpInterface} 零查库读取。网关只做登录态校验（d6c2a08 教训：
 * 网关 classpath 无 StpInterface，checkRole 恒空、全员 403）。</p>
 *
 * <p>由 {@link SaTokenPermissionAutoConfiguration} 在服务配置
 * {@code pinda.auth.required-roles} 时装配，多角色 OR 关系（任一命中即放行，
 * 通常写"本端角色,ADMIN"）。</p>
 */
public class RequiredRoleInterceptor implements HandlerInterceptor {

    private final String[] requiredRoles;

    public RequiredRoleInterceptor(String... requiredRoles) {
        this.requiredRoles = requiredRoles;
    }

    /**
     * 供 {@code extends WebMvcConfigurationSupport} 的服务在
     * {@code addInterceptors} 中显式注册——WebMvcConfigurationSupport 会使 Boot MVC
     * 自动装配退避，pd-common 经 {@code WebMvcConfigurer} 注册的本拦截器不被收集
     * （driver/courier/customer 三端实测失效的根因）。属性空/空白则不注册。
     */
    public static void registerIfConfigured(InterceptorRegistry registry, String requiredRoles) {
        if (requiredRoles == null || requiredRoles.isBlank()) {
            return;
        }
        String[] roles = Arrays.stream(requiredRoles.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
        registry.addInterceptor(new RequiredRoleInterceptor(roles))
                .addPathPatterns("/**")
                .excludePathPatterns("/actuator/**", "/favicon.ico");
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (requiredRoles == null || requiredRoles.length == 0) {
            return true;
        }
        StpUtil.checkRoleOr(requiredRoles);
        return true;
    }
}
