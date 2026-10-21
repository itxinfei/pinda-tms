package com.xinfei.gateway.filter;

import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 请求日志过滤器（T-SEC-01/A2 请求日志 + traceId）
 *
 * <p>职责：</p>
 * <ul>
 *   <li>traceId：入站无 {@code X-Trace-Id} 头则生成（32 位 hex），透传下游 +
 *       回写响应头（前端/排查侧可取到同链 ID）；</li>
 *   <li>访问日志：method / path / 脱敏后 query / 真实IP / userId / status / 耗时 / traceId，
 *       order 在 ForwardSameTokenFilter(-100) 之后，覆盖全部请求；</li>
 *   <li>脱敏（E2）：query 串经 {@link SensitiveMasker} 统一打码后才入日志，
 *       日志文件无明文密码/完整手机号（验收口径）；</li>
 *   <li>探活降噪：/actuator/** 探活请求降为 debug，避免刷屏。</li>
 * </ul>
 *
 * <p>下游服务：SameTokenAuthInterceptor / TokenAuthInterceptor 读取 X-Trace-Id 落 MDC，
 * logback pattern 按 {@code %X{traceId}} 输出，实现全链路日志串联。</p>
 *
 * <p>order=-50：最后入链（前序过滤器已就绪真实 IP / userId 属性），then() 后置统一记录耗时。</p>
 */
@Slf4j
@Component
public class RequestLogFilter implements GlobalFilter, Ordered {

    private static final String TRACE_HEADER = "X-Trace-Id";

    // ---- 日志脱敏正则（E2）----
    // password/token/captcha/secret 类键值对：值整体 ***
    private static final Pattern KV_SENSITIVE = Pattern.compile(
            "(?i)((?:password|passwd|pwd|token|captcha|secret|access_?key|api_?key)\\s*[=:]\\s*)(\"?)([^\"&\\s,}]+)\\2");
    // 11 位手机号：保留前 3 后 4
    private static final Pattern MOBILE = Pattern.compile("(?<![0-9])(1[3-9]\\d)\\d{4}(\\d{4})(?![0-9])");
    // 18 位身份证：保留前 6 后 4
    private static final Pattern ID_CARD = Pattern.compile(
            "(?<![0-9Xx])(\\d{6})\\d{8}(\\d{3}[0-9Xx])(?![0-9Xx])");

    /**
     * query 串脱敏（E2）。
     * 口径与 xfwl-common SensitiveMasker 一致；网关不依赖 xfwl-common（见 SaTokenConfigure 说明），
     * 故在网关侧保留同规则副本——两处规则须同步维护。
     */
    private static String mask(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String out = KV_SENSITIVE.matcher(text).replaceAll("$1$2***$2");
        out = MOBILE.matcher(out).replaceAll("$1****$2");
        out = ID_CARD.matcher(out).replaceAll("$1********$2");
        return out;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String traceId = request.getHeaders().getFirst(TRACE_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        final String tid = traceId;

        String realIp = exchange.getAttribute(RequestSanitizeFilter.ATTR_REAL_IP);
        if (realIp == null || realIp.isBlank()) {
            realIp = request.getRemoteAddress() == null
                    ? "unknown"
                    : String.valueOf(request.getRemoteAddress().getAddress());
        }
        final String ip = realIp;
        final String userId = resolveUserId(request);
        final String method = request.getMethod() == null ? "-" : request.getMethod().name();
        final String path = request.getPath().value();
        final String rawQuery = request.getURI().getRawQuery();
        final long start = System.currentTimeMillis();

        // 响应头回写 traceId（排障时客户端可取证）
        exchange.getResponse().getHeaders().set(TRACE_HEADER, tid);

        ServerHttpRequest mutated = request.mutate().header(TRACE_HEADER, tid).build();
        return chain.filter(exchange.mutate().request(mutated).build())
                .then(Mono.fromRunnable(() -> {
                    org.springframework.http.HttpStatusCode statusCode = exchange.getResponse().getStatusCode();
                    int status = statusCode == null ? 200 : statusCode.value();
                    long cost = System.currentTimeMillis() - start;
                    String query = rawQuery == null ? "" : "?" + mask(rawQuery);
                    if (path.startsWith("/actuator")) {
                        // 探活降噪
                        if (log.isDebugEnabled()) {
                            log.debug("[网关] {} {}{} ip={} user={} status={} cost={}ms traceId={}",
                                    method, path, query, ip, userId, status, cost, tid);
                        }
                        return;
                    }
                    MDC.put("traceId", tid);
                    try {
                        log.info("[网关] {} {}{} ip={} user={} status={} cost={}ms traceId={}",
                                method, path, query, ip, userId, status, cost, tid);
                    } finally {
                        MDC.remove("traceId");
                    }
                }));
    }

    @Override
    public int getOrder() {
        return -50;
    }

    private String resolveUserId(ServerHttpRequest request) {
        String token = request.getHeaders().getFirst("token");
        if (token == null || token.isEmpty()) {
            return null;
        }
        try {
            Object loginId = StpUtil.getLoginIdByToken(token);
            return loginId == null ? null : String.valueOf(loginId);
        } catch (Exception e) {
            return null;
        }
    }
}
