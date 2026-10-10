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
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Set;

/**
 * MyBatis-Plus 插件配置：多租户 + 分页。
 *
 * 多租户规则：业务表自动拼接 tenant_id；平台级表（租户主表、菜单、资源、行政区划）全局共享，放行。
 * 租户来源：登录态读 token-session；内部 Feign 免登录请求读上游透传的 tenantId 请求头。
 */
@Configuration
public class MybatisPlusConfigure {

    /** 租户ID在业务表中的列名 */
    public static final String TENANT_COLUMN = "tenant_id";

    /** token-session 中租户ID的键 */
    public static final String SESSION_TENANT_ID = "tenantId";

    /** 内部 Feign 请求透传租户的请求头 */
    public static final String TENANT_HEADER = "tenantId";

    /** 平台级共享表（不带租户隔离） */
    private static final Set<String> IGNORE_TABLES = Set.of(
            "sys_tenant",
            "sys_menu",
            "sys_resource",
            // 行政区划为国家标准全局数据，所有租户共享
            "dict_area"
    );

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override
            public net.sf.jsqlparser.expression.Expression getTenantId() {
                // 1. 登录态：token-session
                try {
                    Object tenantId = StpUtil.getTokenSession().get(SESSION_TENANT_ID);
                    if (tenantId instanceof Number number) {
                        return new LongValue(number.longValue());
                    }
                } catch (Exception ignore) {
                    // 未登录（内部免登录请求）转下方 header 解析
                }
                // 2. 内部 Feign 请求：上游透传的 tenantId 头
                try {
                    ServletRequestAttributes attributes =
                            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                    if (attributes != null) {
                        String header = attributes.getRequest().getHeader(TENANT_HEADER);
                        if (StringUtils.hasText(header)) {
                            return new LongValue(Long.parseLong(header.trim()));
                        }
                    }
                } catch (Exception ignore) {
                    // 头缺失或格式异常，回落 0
                }
                // 取不到租户时回落 0，不泄露任何租户数据
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
