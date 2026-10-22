package com.xinfei.gateway.filter;

import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * 网关限流过滤器（T-SEC-01/A1，覆盖 69 号文档 A1 / A19 兜底）
 *
 * <p>Redis 固定窗口计数（同 Redis 共享计数，多网关实例限流天然共享），四档：</p>
 * <ul>
 *   <li>登录接口 /api/auth/anno/login：{login-per-minute:30} 次/分/IP（防暴力破解/撞库）；</li>
 *   <li>验证码接口 /api/auth/anno/captcha：{captcha-per-minute:30} 次/分/IP（防短信/图形轰炸）；</li>
 *   <li>登录用户：{user-per-minute:1000} 次/分/user（token 转出 userId）；</li>
 *   <li>IP 兜底：{ip-per-minute:5000} 次/分/IP（真实 IP 来自 RequestSanitizeFilter，防伪造 XFF）。</li>
 * </ul>
 *
 * <p>阈值为 dev 演示口径（办公室 NAT 同出口 IP 多人共用，阈值过严会误伤正常登录/轮询——
 * 安全措施不得影响正常业务）；生产上线时经 Nacos 收紧（login 5/captcha 10/user 300/ip 2000）。</p>
 *
 * <p>超限统一 429 JSON（含 retryAfter）；Redis 异常 fail-open（告警日志，不阻断业务——
 * 限流是防御项，不能反过来成为单点故障）。阈值经 yml/Nacos 可调（禁硬编码纪律）。</p>
 *
 * <p>order=-250：晚于 RequestSanitizeFilter(-300)（真实 IP 已就绪）、SecurityHeadersFilter(-260)
 * （安全头已设置），早于 ForwardSameTokenFilter(-100)。</p>
 */
@Slf4j
@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    private static final String ATTR_USER_ID = "gateway.userId";

    private final ReactiveStringRedisTemplate redis;

    // dev 演示口径（办公室 NAT 多人共用出口 IP，防误伤正常业务）；生产经 Nacos 收紧
    @Value("${xinfei.security.rate-limit.login-per-minute:30}")
    private long loginPerMinute;

    @Value("${xinfei.security.rate-limit.captcha-per-minute:30}")
    private long captchaPerMinute;

    @Value("${xinfei.security.rate-limit.user-per-minute:1000}")
    private long userPerMinute;

    @Value("${xinfei.security.rate-limit.ip-per-minute:5000}")
    private long ipPerMinute;

    public RateLimitFilter(ReactiveStringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // CORS 预检不计数
        if (HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())) {
            return chain.filter(exchange);
        }
        String ip = exchange.getAttribute(RequestSanitizeFilter.ATTR_REAL_IP);
        if (ip == null || ip.isBlank()) {
            ip = exchange.getRequest().getRemoteAddress() == null
                    ? "unknown"
                    : String.valueOf(exchange.getRequest().getRemoteAddress().getAddress());
        }
        String path = exchange.getRequest().getPath().value().toLowerCase();

        // 敏感匿名接口：按 IP 严限（防撞库/验证码轰炸）
        if (path.startsWith("/api/auth/anno/login")) {
            return check(exchange, chain, new Counter("login", ip, loginPerMinute));
        }
        if (path.startsWith("/api/auth/anno/captcha")) {
            return check(exchange, chain, new Counter("captcha", ip, captchaPerMinute));
        }

        // 通用：IP 兜底 + 登录用户双计数
        String userId = resolveUserId(exchange);
        if (userId != null) {
            // ConcurrentHashMap 禁止 null value，匿名请求（userId=null）不得写入
            exchange.getAttributes().put(ATTR_USER_ID, userId);
        }
        String epochMinute = String.valueOf(System.currentTimeMillis() / 60000L);
        Counter ipCounter = new Counter("ip", ip + ":" + epochMinute, ipPerMinute);
        if (userId == null) {
            return check(exchange, chain, ipCounter);
        }
        return check(exchange, chain, ipCounter, new Counter("user", userId + ":" + epochMinute, userPerMinute));
    }

    @Override
    public int getOrder() {
        return -250;
    }

    /**
     * 依次执行计数器（任一超限即 429），全部通过放行
     */
    private Mono<Void> check(ServerWebExchange exchange, GatewayFilterChain chain, Counter... counters) {
        Mono<Boolean> flow = Mono.just(Boolean.TRUE);
        for (Counter counter : counters) {
            flow = flow.flatMap(ok -> ok ? incrementAndCheck(counter) : Mono.just(Boolean.FALSE));
        }
        return flow.flatMap(ok -> ok
                ? chain.filter(exchange)
                : tooManyRequests(exchange));
    }

    /**
     * 单计数器自增并判断是否超限；首次自增设置 70s 过期（窗口 60s + 冗余）。
     * Redis 异常 fail-open 放行并告警。
     */
    private Mono<Boolean> incrementAndCheck(Counter counter) {
        String key = "xinfei:rl:" + counter.scope() + ":" + counter.key();
        return redis.opsForValue().increment(key)
                .flatMap(v -> v == 1L
                        ? redis.expire(key, Duration.ofSeconds(70)).thenReturn(v)
                        : Mono.just(v))
                .map(v -> v <= counter.limit())
                .onErrorResume(e -> {
                    log.warn("[限流] Redis 计数异常，fail-open 放行: key={}, reason={}", key, e.getMessage());
                    return Mono.just(Boolean.TRUE);
                });
    }

    private Mono<Void> tooManyRequests(ServerWebExchange exchange) {
        long retryAfter = 60 - (System.currentTimeMillis() / 1000L) % 60;
        // Retry-After 头必须在写响应体前设置（响应提交后写头无效）
        exchange.getResponse().getHeaders().set("Retry-After", String.valueOf(retryAfter));
        // 429 统一 JSON（T-SEC-01 验收口径）
        return RequestSanitizeFilter.writeError(exchange,
                org.springframework.http.HttpStatus.TOO_MANY_REQUESTS, 429,
                "请求过于频繁，请稍后重试");
    }

    /**
     * 从 token 解析登录用户ID（匿名返回 null）；解析失败不阻断（下游鉴权兜底）
     */
    private String resolveUserId(ServerWebExchange exchange) {
        String token = exchange.getRequest().getHeaders().getFirst("token");
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

    /**
     * 限流计数器：scope 区分计数命名空间，key 为计数主体，limit 为每分钟阈值
     */
    private record Counter(String scope, String key, long limit) {
    }
}
