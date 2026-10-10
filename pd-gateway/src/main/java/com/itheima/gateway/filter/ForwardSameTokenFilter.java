package com.itheima.gateway.filter;

import cn.dev33.satoken.same.SaSameUtil;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 转发鉴权：请求转发到下游前注入 Same-Token。
 * 子服务校验该凭证，确认请求确实来自网关，堵住绕过网关直连子服务端口的访问。
 */
@Component
public class ForwardSameTokenFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest newRequest = exchange.getRequest().mutate()
                .header(SaSameUtil.SAME_TOKEN, SaSameUtil.getToken())
                .build();
        return chain.filter(exchange.mutate().request(newRequest).build());
    }

    @Override
    public int getOrder() {
        // 在 NettyRoutingFilter 真正转发之前执行
        return -100;
    }
}
