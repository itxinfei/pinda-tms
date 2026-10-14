package com.xinfei.common.interceptors;

import com.xinfei.common.context.AsyncRequestHeaders;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Feign 请求拦截器
 *
 * <p>透传网关注入的用户身份头（userid）与租户头（tenantId），保证 Feign 跨服务调用时
 * 下游可识别当前用户与租户（如 pd-oms PayController 的身份校验依赖 userid 头；
 * pd-oms/pd-user 等多租户插件的 tenant_id 过滤依赖 tenantId 头，缺失时下游回落租户 0
 * 导致查询恒空）。
 *
 * <p>取值顺序：① 当前 HTTP 请求头（请求线程内的 Feign 调用）；② {@link AsyncRequestHeaders}
 * 抓取的快照（{@code CompletableFuture} 等异步线程内的 Feign 富化调用，该线程没有请求上下文）。
 * 两处都缺失时不写头，下游按系统态/租户 0 处理。
 */
@Component
public class RequestHeaderInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate template) {
        try {
            String userId = resolveHeader(AsyncRequestHeaders.USER_ID);
            if (StringUtils.isNotBlank(userId)) {
                template.header(AsyncRequestHeaders.USER_ID, userId);
            }
            String tenantId = resolveHeader(AsyncRequestHeaders.TENANT_ID);
            if (StringUtils.isNotBlank(tenantId)) {
                template.header(AsyncRequestHeaders.TENANT_ID, tenantId);
            }
        } catch (Exception e) {
            // 非 Web 上下文且无异步快照（如纯定时任务）时忽略，不影响调用
        }
    }

    /**
     * 解析要透传的头：请求线程优先取当前请求；异步线程回退取 AsyncRequestHeaders 快照。
     */
    private String resolveHeader(String name) {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String value = request.getHeader(name);
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        // 异步富化线程：RequestContextHolder 为空，回退到提交线程抓的头快照
        return AsyncRequestHeaders.get(name);
    }
}
