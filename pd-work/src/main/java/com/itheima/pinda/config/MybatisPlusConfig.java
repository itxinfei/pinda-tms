package com.itheima.pinda.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.itheima.pinda.common.context.TenantContextHolder;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Mybatis Plus 相关配置类
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 多租户插件必须排在分页插件之前
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new WorkTenantLineHandler()));
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    /**
     * work 域租户处理器：6 张 work_ 表全部带 tenant_id，不排除任何表。
     */
    private static class WorkTenantLineHandler implements TenantLineHandler {

        @Override
        public Expression getTenantId() {
            return new LongValue(resolveTenantId());
        }

        @Override
        public String getTenantIdColumn() {
            return "tenant_id";
        }

        @Override
        public boolean ignoreTable(String tableName) {
            return false;
        }
    }

    /**
     * 解析当前租户ID：
     * <ol>
     *   <li>HTTP 请求头 tenantId（pd-gateway 登录后注入）；</li>
     *   <li>系统态线程上下文 {@link TenantContextHolder}（定时任务/MQ 线程）；</li>
     *   <li>回落 0。</li>
     * </ol>
     */
    private static long resolveTenantId() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            String headerTenantId = attributes.getRequest().getHeader("tenantId");
            if (StringUtils.hasText(headerTenantId)) {
                try {
                    return Long.parseLong(headerTenantId.trim());
                } catch (NumberFormatException ignored) {
                    // 非法头值，落到下一级
                }
            }
        }
        Long contextTenantId = TenantContextHolder.getTenantId();
        return contextTenantId != null ? contextTenantId : 0L;
    }
}
