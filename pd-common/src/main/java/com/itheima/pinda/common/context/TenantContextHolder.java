package com.itheima.pinda.common.context;

/**
 * 系统态租户上下文：为没有 HTTP 请求的线程（如 Quartz 定时任务、MQ 消费线程）携带租户ID。
 *
 * <p>HTTP 请求链路优先从网关注入的 tenantId 请求头读取租户；定时任务等场景在执行前
 * {@link #setTenantId(Long)}，执行结束必须在 finally 中 {@link #clear()}，避免线程复用串租户。</p>
 */
public final class TenantContextHolder {

    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();

    private TenantContextHolder() {
    }

    public static void setTenantId(Long tenantId) {
        TENANT_ID.set(tenantId);
    }

    public static Long getTenantId() {
        return TENANT_ID.get();
    }

    public static void clear() {
        TENANT_ID.remove();
    }
}
