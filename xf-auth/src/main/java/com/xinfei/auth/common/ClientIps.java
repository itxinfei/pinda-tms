package com.xinfei.auth.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

/**
 * 客户端真实 IP 提取：优先网关/代理注入的转发头，逐级取第一个非 unknown 地址。
 */
public final class ClientIps {

    private static final String[] HEADERS = {
            "X-Real-IP", "X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP"
    };

    private ClientIps() {
    }

    public static String resolve(HttpServletRequest request) {
        for (String header : HEADERS) {
            String value = request.getHeader(header);
            if (StringUtils.hasText(value) && !"unknown".equalsIgnoreCase(value)) {
                int comma = value.indexOf(',');
                return comma > 0 ? value.substring(0, comma).trim() : value.trim();
            }
        }
        return request.getRemoteAddr();
    }
}
