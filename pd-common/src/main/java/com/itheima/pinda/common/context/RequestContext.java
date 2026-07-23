package com.itheima.pinda.common.context;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Slf4j
public class RequestContext {
    private static final String USER_ID = "userid";
    private static final String USER_NAME = "name";
    private static final String STATION_ID = "stationid";

    private static HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return null;
        return attrs.getRequest();
    }

    public static String getUserId() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return null;
        }
        String userid = request.getHeader(USER_ID);
        log.debug("获取上下文用户id：{}", userid);
        return userid;
    }

    /**
     * 获取当前操作人名称
     *
     * <p>网关（TokenContextFilter）在校验 JWT 后会把用户名写入 {@code name} 请求头并向下游透传，
     * 因此此处可直接从请求头读取真实操作人名称，避免把用户 ID 误写为名称。</p>
     *
     * @return 操作人名称；非 Web 上下文（如异步/定时任务）或缺少该头时返回 null
     */
    public static String getUserName() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return null;
        }
        String username = request.getHeader(USER_NAME);
        log.debug("获取上下文用户名：{}", username);
        return username;
    }

    /**
     * 获取当前操作人岗位ID（stationId）
     *
     * <p>网关在校验 JWT 后会把岗位ID写入 {@code stationid} 请求头并向下游透传，
     * 可用于推导操作人类型（司机/快递员/管理员）。</p>
     *
     * @return 岗位ID；非 Web 上下文、缺少该头或格式非法时返回 null
     */
    public static Long getStationId() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return null;
        }
        String stationId = request.getHeader(STATION_ID);
        if (stationId == null || stationId.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(stationId);
        } catch (NumberFormatException e) {
            log.warn("解析岗位ID失败：{}", stationId);
            return null;
        }
    }
}
