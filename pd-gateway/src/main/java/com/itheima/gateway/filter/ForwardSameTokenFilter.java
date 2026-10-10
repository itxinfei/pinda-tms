package com.itheima.gateway.filter;

import cn.dev33.satoken.same.SaSameUtil;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 转发鉴权：请求转发到下游前注入：
 * 1) Same-Token：子服务据此确认请求来自网关，堵住绕过网关直连子服务；
 * 2) tenantId：已登录请求按 token 查 token-session 取出租户ID透传，供下游 /internal 免登录端点解析租户。
 */
@Component
public class ForwardSameTokenFilter implements GlobalFilter, Ordered {

    public static final String TOKEN_HEADER = "token";
    public static final String TENANT_HEADER = "tenantId";
    public static final String USER_ID_HEADER = "userid";
    public static final String SESSION_TENANT_ID = "tenantId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest.Builder builder = exchange.getRequest().mutate()
                .header(SaSameUtil.SAME_TOKEN, SaSameUtil.getToken());

        // 已登录请求：按 token 查 token-session，透传租户ID（不依赖 Reactor 上下文持有者）
        String tokenValue = exchange.getRequest().getHeaders().getFirst(TOKEN_HEADER);
        if (tokenValue != null && !tokenValue.isEmpty()) {
            try {
                Object tenantId = StpUtil.getTokenSessionByToken(tokenValue)
                        .get(SESSION_TENANT_ID);
                if (tenantId != null) {
                    builder.header(TENANT_HEADER, String.valueOf(tenantId));
                }
            } catch (Exception ignore) {
                // token 失效等不阻断转发，下游鉴权自行处理
            }

            // 透传登录用户ID，供下游 TokenAuthInterceptor 兜底校验（按 token 直查，不依赖 Reactor 上下文）
            Object loginId = StpUtil.getLoginIdByToken(tokenValue);
            if (loginId != null) {
                builder.header(USER_ID_HEADER, String.valueOf(loginId));
            }
        }

        return chain.filter(exchange.mutate().request(builder.build()).build());
    }

    @Override
    public int getOrder() {
        // 在 NettyRoutingFilter 真正转发之前执行
        return -100;
    }
}
