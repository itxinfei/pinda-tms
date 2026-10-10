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

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class PdCompletableFuture {


    public static final CompletableFuture<Map<Long, AreaDTO>> areaMapFuture(AreaFeign api, Long parentId, Set<Long> areaSet) {
        List<AreaDTO> result = api.findAll(parentId, new ArrayList<>(areaSet));
        // 修改点：远程调用结果可能为 null（fallback），统一通过 Rx 安全取值，避免 NPE
        return CompletableFuture.supplyAsync(() ->
                Rx.list(result).stream().collect(Collectors.toMap(AreaDTO::getId, vo -> vo)));
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
        return CompletableFuture.supplyAsync(() -> {
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
        return CompletableFuture.supplyAsync(() -> {
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
        return CompletableFuture.supplyAsync(() -> {
            // 修改点：Feign 直接返回 List 可能为 null，统一通过 Rx 安全取值
            List<TransportOrderDTO> result = Rx.list(api.findByOrderIds(new ArrayList<>(orderSet)));
            return result.stream().collect(Collectors.toMap(TransportOrderDTO::getOrderId, item -> item, (v1, v2) -> v1));
        });
    }
}
