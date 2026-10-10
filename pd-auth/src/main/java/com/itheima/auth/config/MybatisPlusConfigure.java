package com.itheima.auth.config;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

/**
 * MyBatis-Plus 插件配置：多租户 + 分页。
 *
 * 多租户规则：业务表自动拼接 tenant_id；平台级表（租户主表、菜单、资源）全局共享，放行。
 * 登录发生在认证之前，登录链路上的 Mapper 用 @InterceptorIgnore(tenantLine="1") 手动处理租户条件。
 */
@Configuration
public class MybatisPlusConfigure {

    /** 租户ID在业务表中的列名 */
    public static final String TENANT_COLUMN = "tenant_id";

    /** 平台级共享表（不带租户隔离） */
    private static final Set<String> IGNORE_TABLES = Set.of(
            "pd_auth_tenant",
            "pd_auth_menu",
            "pd_auth_resource",
            // 行政区划为国家标准全局数据，所有租户共享
            "pd_area"
    );

    /** token-session 中租户ID的键 */
    public static final String SESSION_TENANT_ID = "tenantId";

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override
            public net.sf.jsqlparser.expression.Expression getTenantId() {
                Object tenantId = StpUtil.getTokenSession().get(SESSION_TENANT_ID);
                if (tenantId instanceof Number number) {
                    return new LongValue(number.longValue());
                }
                // 未登录上下文（理论上业务 Mapper 均已 ignore），回落 0，不泄露任何租户数据
                return new LongValue(0L);
            }

            @Override
            public String getTenantIdColumn() {
                return TENANT_COLUMN;
            }

            @Override
            public boolean ignoreTable(String tableName) {
                return IGNORE_TABLES.contains(tableName);
            }
        }));
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
