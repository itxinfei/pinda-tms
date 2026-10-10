package com.itheima.pinda.feign.interceptor;

import cn.dev33.satoken.same.SaSameUtil;
import com.itheima.pinda.common.context.TenantContextHolder;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 服务间 Feign 出站拦截器：
 * <ol>
 *   <li>注入 Same-Token，使对 pd-auth 的内部调用通过其 Same-Token 校验（其它服务不校验，多注入无害）；</li>
 *   <li>透传 tenantId 头，供 pd-auth 内部端点在免登录场景下解析租户。</li>
 * </ol>
 *
 * 仅依赖 sa-token-core；Same-Token 的 Redis 共享由调用方引入 sa-token starter + redis-jackson
 * 并配置同一 Redis 来保证。
 */
@Slf4j
@Component
public class InternalAuthRequestInterceptor implements RequestInterceptor {

    public static final String TENANT_HEADER = "tenantId";

    @Override
    public void apply(RequestTemplate template) {
        // 1. 注入 Same-Token
        try {
            template.header(SaSameUtil.SAME_TOKEN, SaSameUtil.getToken());
        } catch (Exception e) {
            // Redis 暂不可用不阻断请求构造；pd-auth 强校验失败会以 401 触发 fallback，可被感知
            log.debug("注入 Same-Token 失败: {}", e.getMessage());
        }
        // 2. 透传租户ID
        // 用户态（HTTP 链路）：优先读网关注入的 tenantId 请求头；
        // 系统态（Quartz 等无线程请求的场景）：请求头缺失时回退读 TenantContextHolder。
        try {
            String tenantId = null;
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                tenantId = attributes.getRequest().getHeader(TENANT_HEADER);
            }
            if (!StringUtils.hasText(tenantId)) {
                Long contextTenantId = TenantContextHolder.getTenantId();
                if (contextTenantId != null) {
                    tenantId = String.valueOf(contextTenantId);
                }
            }
            if (StringUtils.hasText(tenantId)) {
                template.header(TENANT_HEADER, tenantId);
            }
        } catch (Exception e) {
            log.debug("透传 tenantId 失败: {}", e.getMessage());
        }
    }
}
