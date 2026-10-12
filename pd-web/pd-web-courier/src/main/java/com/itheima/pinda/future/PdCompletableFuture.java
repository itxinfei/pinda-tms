package com.itheima.pinda.future;

import com.itheima.pinda.DTO.AreaDTO;
import com.itheima.pinda.DTO.OrderDTO;
import com.itheima.pinda.DTO.OrgDTO;
import com.itheima.pinda.DTO.TransportOrderDTO;
import com.itheima.pinda.feign.AreaFeign;
import com.itheima.pinda.feign.OrgFeign;
import com.itheima.pinda.feign.OrderFeign;
import com.itheima.pinda.feign.TransportOrderFeign;
import com.itheima.pinda.util.Rx;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class PdCompletableFuture {

    /**
     * 在异步线程执行 Feign 调用，并透传主线程的 {@link RequestAttributes}（含网关注入的
     * tenantId / userid 请求头）。
     *
     * <p>背景：{@code CompletableFuture.supplyAsync} 默认在 ForkJoinPool 线程执行，
     * 而 {@link RequestContextHolder} 基于 ThreadLocal、不会自动传递。异步线程里
     * RequestHeaderInterceptor 取不到当前请求，Feign 调用不带 tenantId 头，
     * 下游（pd-oms）多租户插件回落 {@code tenant_id=0}，按 id 批量查询恒为空，
     * 快递员取件/派件任务列表因此在组装 DTO 时 NPE 返回 500。</p>
     *
     * <p>做法：在主线程（Tomcat 线程，请求上下文仍在）提交任务前捕获 RequestAttributes，
     * 异步任务体内 set 进当前线程、finally 里 reset，避免线程复用串上下文。</p>
     */
    private static <T> CompletableFuture<T> asyncWithContext(Supplier<T> supplier) {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        return CompletableFuture.supplyAsync(() -> {
            RequestContextHolder.setRequestAttributes(attributes);
            try {
                return supplier.get();
            } finally {
                RequestContextHolder.resetRequestAttributes();
            }
        });
    }

    public static final CompletableFuture<Map<Long, AreaDTO>> areaMapFuture(AreaFeign api, Long parentId, Set<Long> areaSet) {
        return asyncWithContext(() -> {
            // 修改点：远程调用结果可能为 null（fallback），统一通过 Rx 安全取值，避免 NPE
            List<AreaDTO> result = Rx.list(api.findAll(parentId, new ArrayList<>(areaSet)));
            return result.stream().collect(Collectors.toMap(AreaDTO::getId, vo -> vo));
        });
    }

    /**
     * 获取机构数据列表
     *
     * @param api        数据接口
     * @param agencyType 机构类型
     * @param ids        机构id列表
     * @return 执行结果
     */
    public static final CompletableFuture<Map<Long, OrgDTO>> agencyMapFuture(OrgFeign api, Integer agencyType, Set<Long> ids, Long countyId) {
        return asyncWithContext(() -> {
            // 修改点：Feign 裸返回可能为 null，统一通过 Rx 安全取值，避免 NPE
            List<OrgDTO> result = Rx.list(api.list(agencyType, new ArrayList<>(ids), countyId, null, null));
            return result.stream().collect(Collectors.toMap(OrgDTO::getId, org -> org));
        });
    }

    /**
     * 批量获取订单信息
     *
     * @param api
     * @param orderSet
     * @return
     */
    public static CompletableFuture<Map<String, OrderDTO>> orderMapFuture(OrderFeign api, Set<Long> orderSet) {
        return asyncWithContext(() -> {
            // 修改点：Feign 直接返回 List 可能为 null，统一通过 Rx 安全取值；OrderFeign 仍为 String 契约，边界转换
            List<OrderDTO> result = Rx.list(api.findByIds(orderSet.stream().map(String::valueOf).collect(Collectors.toList())));
            return result.stream().collect(Collectors.toMap(OrderDTO::getId, item -> item));
        });
    }

    /**
     * 批量获取运单信息
     *
     * @param api
     * @param orderSet
     * @return
     */
    public static CompletableFuture<Map<Long, TransportOrderDTO>> tranOrderMapFuture(TransportOrderFeign api, Set<Long> orderSet) {
        return asyncWithContext(() -> {
            // 修改点：Feign 直接返回 List 可能为 null，统一通过 Rx 安全取值
            List<TransportOrderDTO> result = Rx.list(api.findByOrderIds(new ArrayList<>(orderSet)));
            return result.stream().collect(Collectors.toMap(TransportOrderDTO::getOrderId, item -> item, (v1, v2) -> v1));
        });
    }
}
