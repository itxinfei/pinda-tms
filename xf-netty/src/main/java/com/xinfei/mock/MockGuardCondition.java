package com.xinfei.mock;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * mock 数据生成器装配守门（2026-10-19 用户需求：dev/test 手动生成测试数据，生产必须关闭）。
 *
 * <p>三把锁全过才装配，任一缺失整个 mock 包不注册、端点 404：</p>
 * <ol>
 *   <li>Spring 配置 {@code xinfei.mock.generator.enabled=true}——该 key 仅存在于
 *       dev 环境的 Nacos 配置（xinfei-tms 组），生产配置无此 key；</li>
 *   <li>系统环境变量 {@code XINFEI_MOCK_ENABLED=true}——仅 dev compose 注入，
 *       走 {@link System#getenv} 而非 Spring Environment，与 Nacos 配置完全隔离，
 *       防止 dev 配置被导出/复制到生产时单点失效；</li>
 *   <li>激活 profile 含 prod 时一票否决（即使前两把锁同时误配）。</li>
 * </ol>
 */
public class MockGuardCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        for (String profile : context.getEnvironment().getActiveProfiles()) {
            if ("prod".equalsIgnoreCase(profile)) {
                return false;
            }
        }
        boolean nacosSwitch = "true".equalsIgnoreCase(
                context.getEnvironment().getProperty("xinfei.mock.generator.enabled"));
        boolean envSwitch = "true".equalsIgnoreCase(System.getenv("XINFEI_MOCK_ENABLED"));
        return nacosSwitch && envSwitch;
    }
}
