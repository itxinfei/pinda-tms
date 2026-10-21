package com.xinfei.gateway.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 安全响应头过滤器（T-SEC-02/A13）
 *
 * <p>对全部网关响应统一注入：HSTS / X-Frame-Options: DENY / nosniff /
 * Referrer-Policy / CSP；司机/快递员端路径追加 {@code Permissions-Policy: geolocation=(self)}
 * （定位上报是核心功能，其余端不开放地理权限）。</p>
 *
 * <p>CSP 白名单走配置（{@code xinfei.security.csp}，Nacos 可覆盖），管理端下单页地图需放行
 * *.baidu.com / *.bcebos.com，大屏 WS 需放行 wss。<b>铁律：CSP 配置错误会直接挡掉正常页面资源，
 * 因此只在显式配置时才下发——dev 默认不下发（不影响业务）；生产上线前配置白名单串后，
 * 必须逐端回归三条用例：管理端下单页地图 / 大屏 WS / 司机端定位授权（《71》T-SEC-02 验收），
 * 并将新增域名登记（《69》A13）。</b></p>
 */
@Component
public class SecurityHeadersFilter implements GlobalFilter, Ordered {

    /** 移动端定位路径前缀：司机/快递员 BFF */
    private static final String[] GEO_PATH_PREFIXES = {"/api/web-driver", "/api/web-courier"};

    @Value("${xinfei.security.csp:}")
    private String csp;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        HttpHeaders headers = exchange.getResponse().getHeaders();
        headers.set("X-Frame-Options", "DENY");
        headers.set("X-Content-Type-Options", "nosniff");
        headers.set("Referrer-Policy", "strict-origin-when-cross-origin");
        // 非 TLS 响应浏览器忽略该头，无害；T-SEC-06 HTTPS 落地后即生效
        headers.set("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        // CSP 仅在显式配置时下发（配置错误会挡正常资源，dev 不下发不影响业务）
        if (csp != null && !csp.isBlank()) {
            headers.set("Content-Security-Policy", csp);
        }
        String path = exchange.getRequest().getPath().value();
        for (String prefix : GEO_PATH_PREFIXES) {
            if (path.startsWith(prefix)) {
                headers.set("Permissions-Policy", "geolocation=(self)");
                break;
            }
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        // 先于限流/转发设置响应头（响应未提交前均可写）
        return -260;
    }
}
