package com.xinfei.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * 入站请求净化过滤器（T-SEC-02/A3 头剥离 + T-SEC-01/A19 URL 过滤）
 *
 * <p>职责（全部请求的统一安全前置，先于限流/鉴权/转发）：</p>
 * <ul>
 *   <li>A19 URL 过滤：超长 URL（&gt;2048）→ 414；路径含 {@code ..} / {@code %2e}（路径穿越）、
 *       控制字符 → 400；</li>
 *   <li>A3 头剥离：删除全部入站 {@code X-Forwarded-*} / {@code X-Real-IP}（防伪造），
 *       按可信代理解析真实客户端 IP 后重写 {@code X-Real-IP} 供下游与限流计数使用——
 *       仅当 TCP 对端是内网可信代理（RFC1918/回环，即 Nginx/容器网）时才信任其 XFF 首段，
 *       公网直连伪造的 XFF 一律忽略，保证 T-SEC-01 限流计数归属不被伪造头污染；</li>
 *   <li>规格限制：请求头总量 &gt;8KB → 400；Content-Length &gt;10MB → 413
 *       （chunked 无 Content-Length 无法廉价预检，由下游框架限制兜底）。</li>
 * </ul>
 *
 * <p>order=-300：早于 SecurityHeadersFilter(-260)、RateLimitFilter(-250)、
 * ForwardSameTokenFilter(-100)、RequestLogFilter(-50)。</p>
 */
@Component
public class RequestSanitizeFilter implements GlobalFilter, Ordered {

    /** exchange 属性键：解析后的真实客户端 IP（RateLimitFilter/RequestLogFilter 消费） */
    public static final String ATTR_REAL_IP = "gateway.realIp";

    private static final int MAX_URL_LENGTH = 2048;
    private static final int MAX_HEADER_BYTES = 8 * 1024;
    /** 10MB：须大于正常文件上传/Excel 导入体积（与 Boot 默认 multipart 上限同量级），
     *  只拦明显异常的巨型请求体，精拦交给下游框架 multipart 配置兜底——防误伤正常上传业务 */
    private static final long MAX_BODY_BYTES = 10L * 1024L * 1024L;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // ---- A19：URL 长度 / 路径穿越 / 控制字符 ----
        String rawPath = request.getURI().getRawPath() == null ? "" : request.getURI().getRawPath();
        String path = request.getPath().value();
        if (rawPath.length() > MAX_URL_LENGTH) {
            return writeError(exchange, HttpStatus.URI_TOO_LONG, 414, "URI Too Long");
        }
        String lower = rawPath.toLowerCase();
        if (lower.contains("..") || lower.contains("%2e") || hasControlChar(rawPath)) {
            return writeError(exchange, HttpStatus.BAD_REQUEST, 400, "非法请求路径");
        }
        path = path.toLowerCase();
        if (path.contains("..") && !lower.contains("..")) {
            return writeError(exchange, HttpStatus.BAD_REQUEST, 400, "非法请求路径");
        }

        // ---- 规格限制：请求头总量 / 请求体长度 ----
        int headerBytes = 0;
        for (String name : request.getHeaders().keySet()) {
            headerBytes += name.length();
            for (String value : request.getHeaders().get(name)) {
                headerBytes += value == null ? 0 : value.length();
            }
        }
        if (headerBytes > MAX_HEADER_BYTES) {
            return writeError(exchange, HttpStatus.BAD_REQUEST, 400, "请求头过大");
        }
        String contentLength = request.getHeaders().getFirst("Content-Length");
        if (contentLength != null) {
            try {
                if (Long.parseLong(contentLength.trim()) > MAX_BODY_BYTES) {
                    return writeError(exchange, HttpStatus.PAYLOAD_TOO_LARGE, 413, "请求体过大");
                }
            } catch (NumberFormatException ignore) {
                // 非法 Content-Length 交由框架解析报错
            }
        }

        // ---- A3：剥离伪造头，按可信代理解析真实 IP ----
        String remoteAddr = null;
        InetSocketAddress remote = request.getRemoteAddress();
        if (remote != null && remote.getAddress() != null) {
            remoteAddr = remote.getAddress().getHostAddress();
        }
        String xff = request.getHeaders().getFirst("X-Forwarded-For");
        String realIp;
        if (xff != null && !xff.isBlank() && isTrustedProxy(remoteAddr)) {
            // 可信代理（Nginx/容器网）追加的 XFF：首段为真实客户端 IP
            realIp = xff.split(",")[0].trim();
        } else {
            // 无代理或公网直连：以 TCP 对端为准（忽略客户端自带 XFF，防伪造）
            realIp = remoteAddr == null ? "unknown" : remoteAddr;
        }
        exchange.getAttributes().put(ATTR_REAL_IP, realIp);
        ServerHttpRequest mutated = request.mutate()
                .headers(hs -> {
                    hs.remove("X-Forwarded-For");
                    hs.remove("X-Forwarded-Proto");
                    hs.remove("X-Forwarded-Host");
                    hs.remove("X-Forwarded-Port");
                    hs.remove("X-Forwarded-Prefix");
                    hs.remove("X-Real-IP");
                })
                .header("X-Real-IP", realIp)
                .build();
        return chain.filter(exchange.mutate().request(mutated).build());
    }

    @Override
    public int getOrder() {
        return -300;
    }

    /**
     * 是否可信代理（内网）：回环 + RFC1918（10/8、172.16/12、192.168/16）。
     * dev 全内网部署成立；生产 Nginx 前置于同网段亦满足（T-SEC-06）。
     */
    private boolean isTrustedProxy(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        if (ip.startsWith("127.") || "0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return true;
        }
        if (ip.startsWith("10.") || ip.startsWith("192.168.")) {
            return true;
        }
        if (ip.startsWith("172.")) {
            try {
                int second = Integer.parseInt(ip.split("\\.")[1]);
                return second >= 16 && second <= 31;
            } catch (Exception ignore) {
                return false;
            }
        }
        return false;
    }

    private boolean hasControlChar(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) < 0x20) {
                return true;
            }
        }
        return false;
    }

    /**
     * 统一错误 JSON 响应（RateLimitFilter 复用）
     */
    static Mono<Void> writeError(ServerWebExchange exchange, HttpStatus status, int code, String msg) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"code\":" + code + ",\"msg\":\"" + msg + "\"}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
