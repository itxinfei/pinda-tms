package com.itheima.pinda.common.interceptors;

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
 */
@Component
public class RequestHeaderInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate template) {
        // 透传网关注入的身份/租户头，避免 Feign 调用被下游身份门禁误判为匿名、或多租户过滤落空
        try {
            ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String userId = request.getHeader("userid");
                if (StringUtils.isNotBlank(userId)) {
                    template.header("userid", userId);
                }
                String tenantId = request.getHeader("tenantId");
                if (StringUtils.isNotBlank(tenantId)) {
                    template.header("tenantId", tenantId);
                }
            }
        } catch (Exception e) {
            // 非 Web 上下文（如定时任务/异步）时忽略，不影响调用
        }
    }
}
