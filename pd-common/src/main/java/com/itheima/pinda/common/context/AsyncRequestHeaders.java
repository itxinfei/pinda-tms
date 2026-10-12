package com.itheima.pinda.common.context;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * 异步线程透传网关注入请求头的工具。
 *
 * <p>背景：{@code CompletableFuture.supplyAsync} 默认在 ForkJoinPool 线程执行，而
 * {@link RequestContextHolder} 基于 ThreadLocal，<b>不会</b>把当前请求的属性传播到异步线程。
 * 于是异步线程内发起的 Feign 调用：
 * <ol>
 *   <li>{@code RequestHeaderInterceptor} 取不到当前请求，无法透传 {@code userid}/{@code tenantId} 头；</li>
 *   <li>下游（如 pd-base）多租户插件收不到 {@code tenantId}，回落租户 0，字典/档案查询恒空，
 *       BFF 富化出来的关联名称全部为 {@code null}（车型关联货型、车辆车型/车队列名等）。</li>
 * </ol>
 *
 * <p>本工具在<b>提交线程（请求线程）</b>把需要透传的头抓成<b>字符串快照</b>（不持有 request 对象，
 * 避免请求结束后复用已回收对象），在异步线程执行前置入 ThreadLocal、执行后恢复/清理。
 * Feign 出口的 {@code RequestHeaderInterceptor} 在没有请求上下文时回退读取该快照。
 *
 * <p><b>强制用法</b>：凡在请求处理过程中、需要在异步线程里发起 Feign 富化调用的，一律用
 * <pre>{@code
 * AsyncRequestHeaders.supplyAsync(() -> feign.findAll(ids));
 * }</pre>
 * 替换裸 {@code CompletableFuture.supplyAsync(...)}，禁止再直接使用后者做跨服务富化。
 */
public final class AsyncRequestHeaders {

    /** 网关注入的用户 ID 头 */
    public static final String USER_ID = "userid";
    /** 网关注入的租户 ID 头 */
    public static final String TENANT_ID = "tenantId";

    /** 需要跨线程透传的头白名单（仅身份/租户，勿在此塞业务头） */
    private static final String[] PROPAGATED_HEADERS = {USER_ID, TENANT_ID};

    private static final ThreadLocal<Map<String, String>> CONTEXT = new ThreadLocal<>();

    private AsyncRequestHeaders() {
    }

    /**
     * 带请求头传播的 {@link CompletableFuture#supplyAsync(Supplier)}。
     *
     * <p>在当前（提交）线程抓取头快照，任务在 ForkJoinPool 线程执行期间可通过
     * {@link #get(String)} 读到这些头。
     */
    public static <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier) {
        // 必须在提交线程抓快照：异步线程里 RequestContextHolder 已为空
        Map<String, String> snapshot = capture();
        return CompletableFuture.supplyAsync(() -> runWith(snapshot, supplier));
    }

    /**
     * 供非 CompletableFuture 场景（如自定义线程池/MQ 消费）显式携带快照执行。
     *
     * @param snapshot 由 {@link #capture()} 在请求线程取得的快照
     */
    public static <U> U runWith(Map<String, String> snapshot, Supplier<U> supplier) {
        Map<String, String> previous = CONTEXT.get();
        CONTEXT.set(snapshot);
        try {
            return supplier.get();
        } finally {
            // 线程会被池化复用，必须还原/清理，杜绝请求间串头、串租户
            if (previous == null) {
                CONTEXT.remove();
            } else {
                CONTEXT.set(previous);
            }
        }
    }

    /**
     * 在请求线程抓取当前需要透传的头快照（不可变）。
     *
     * <p>优先取当前 HTTP 请求头；当前线程本身已是异步线程（嵌套提交）时回退继承父级快照。
     */
    public static Map<String, String> capture() {
        Map<String, String> snapshot = new HashMap<>();
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            for (String header : PROPAGATED_HEADERS) {
                String value = request.getHeader(header);
                if (value != null && !value.trim().isEmpty()) {
                    snapshot.put(header, value.trim());
                }
            }
        } else {
            Map<String, String> parent = CONTEXT.get();
            if (parent != null) {
                snapshot.putAll(parent);
            }
        }
        return Map.copyOf(snapshot);
    }

    /**
     * 读取当前异步线程上的透传头值；不存在时返回 {@code null}。
     * 供 Feign 拦截器在缺少请求上下文时回退使用。
     */
    public static String get(String name) {
        Map<String, String> headers = CONTEXT.get();
        return headers == null ? null : headers.get(name);
    }
}
