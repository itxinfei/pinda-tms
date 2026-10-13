package com.itheima.pinda.common.satoken;

import cn.dev33.satoken.stp.StpInterface;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

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
}
