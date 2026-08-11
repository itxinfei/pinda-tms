package com.itheima.pinda.common.interceptors;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import io.seata.core.context.RootContext;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * Feign 请求拦截器
 *
 * <ol>
 *   <li>透传 Seata XID（标识局部事务所属全局事务）；</li>
 *   <li>透传网关注入的用户身份头（userid），保证 Feign 跨服务调用时下游可识别当前用户
 *       （如 pd-oms PayController 的身份校验依赖 userid 头）。</li>
 * </ol>
 */
@Component
public class RequestHeaderInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate template) {
        // 1. 透传 Seata XID
        String xid = RootContext.getXID();
        if (StringUtils.isNotBlank(xid)) {
            template.header("TX_XID", xid);
        }
        // 2. 透传网关注入的用户身份头（userid），避免 Feign 调用被下游身份门禁误判为匿名
        try {
            ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String userId = request.getHeader("userid");
                if (StringUtils.isNotBlank(userId)) {
                    template.header("userid", userId);
                }
            }
        } catch (Exception e) {
            // 非 Web 上下文（如定时任务/异步）时忽略，不影响调用
        }
    }
}