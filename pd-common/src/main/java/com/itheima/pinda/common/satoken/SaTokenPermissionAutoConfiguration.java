package com.itheima.pinda.common.satoken;

import cn.dev33.satoken.stp.StpInterface;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

/**
 * 共享 Sa-Token 权限装配（#9 草稿）。
 *
 * <p>仅当 classpath 存在 sa-token（即服务引了 sa-token-spring-boot3-starter，如 pd-web-manager、
 * pd-auth）时生效；pd-common 被 23 个模块依赖，但绝大多数业务服务未引 sa-token，
 * 此 {@code @ConditionalOnClass} 保证对它们零影响。</p>
 *
 * <p>{@code @ConditionalOnMissingBean(StpInterface.class)}：pd-auth 自己提供了查库的
 * StpInterfaceImpl，本 Bean 不会覆盖它；BFF 未自定义时才装配共享会话版。</p>
 *
 * <p><b>启用方式</b>：类准备好后，把本类全限定名加入
 * META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
 * 即自动生效（草稿阶段暂不注册，待 web-manager 接入联调时打开）。</p>
 */
@AutoConfiguration
@ConditionalOnClass(StpInterface.class)
public class SaTokenPermissionAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(StpInterface.class)
    public StpInterface sharedSessionStpInterface() {
        return new SharedSessionStpInterface();
    }

    /**
     * 端级角色绑定（X-01 下沉落点）：仅当服务显式配置 {@code pinda.auth.required-roles}
     * 时注册，多角色逗号分隔、OR 关系（通常写"本端角色,ADMIN"）。
     *
     * <p>拦截 /**（登录态已由网关校验，此处只做角色）， actuator 探针必须排除——
     * 不放行会被 HTTP 探针 401 打成 unhealthy，触发 auto-deploy 全量回滚（两次教训）。</p>
     */
    @Bean
    @ConditionalOnProperty(name = "pinda.auth.required-roles")
    public WebMvcConfigurer requiredRoleInterceptorConfigurer(
            @Value("${pinda.auth.required-roles}") String requiredRoles) {
        String[] roles = Arrays.stream(requiredRoles.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(new RequiredRoleInterceptor(roles))
                        .addPathPatterns("/**")
                        .excludePathPatterns("/actuator/**", "/favicon.ico");
            }
        };
    }

    /** 鉴权异常收口（401/403 统一 Result 壳）：与拦截器同条件装配，未配置的服务零影响 */
    @Bean
    @ConditionalOnProperty(name = "pinda.auth.required-roles")
    public SaTokenExceptionHandler saTokenExceptionHandler() {
        return new SaTokenExceptionHandler();
    }
}
