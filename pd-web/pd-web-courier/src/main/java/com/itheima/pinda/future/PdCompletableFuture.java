package com.itheima.pinda.future;

import com.itheima.pinda.DTO.OrderDTO;
import com.itheima.pinda.DTO.TransportOrderDTO;
import com.itheima.pinda.authority.api.AreaApi;
import com.itheima.pinda.authority.api.OrgApi;
import com.itheima.pinda.authority.entity.common.Area;
import com.itheima.pinda.authority.entity.core.Org;
import com.itheima.pinda.base.R;
import com.itheima.pinda.feign.OrderFeign;
import com.itheima.pinda.feign.TransportOrderFeign;
import com.itheima.pinda.util.Rx;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class PdCompletableFuture {


    public static final CompletableFuture<Map<Long, Area>> areaMapFuture(AreaApi api, Long parentId, Set<Long> areaSet) {
        R<List<Area>> result = api.findAll(parentId, new ArrayList<>(areaSet));
        // 修改点：远程调用结果 data 可能为 null，统一通过 Rx 安全取值，避免 NPE
        return CompletableFuture.supplyAsync(() ->
                Rx.dataList(result).stream().collect(Collectors.toMap(Area::getId, vo -> vo)));
    }

    /**
     * 获取机构数据列表
     *
     * @param api        数据接口
     * @param agencyType 机构类型
     * @param ids        机构id列表
     * @return 执行结果
     */
    public static final CompletableFuture<Map<Long, Org>> agencyMapFuture(OrgApi api, Integer agencyType, Set<String> ids, Long countyId) {
        return CompletableFuture.supplyAsync(() -> {
            R<List<Org>> result = api.list(agencyType, ids.stream().mapToLong(id -> Long.valueOf(id)).boxed().collect(Collectors.toList()), countyId, null, null);
            // 修改点：远程调用成功但 data 可能为 null，增加判空避免 NPE
            if (result.getIsSuccess() && result.getData() != null) {
                return result.getData().stream().collect(Collectors.toMap(Org::getId, org -> org));
            }
            return new HashMap<>();
        });
    }

    /**
     * 批量获取订单信息
     *
     * @param api
     * @param orderSet
     * @return
     */
    public static CompletableFuture<Map<String, OrderDTO>> orderMapFuture(OrderFeign api, Set<String> orderSet) {
        return CompletableFuture.supplyAsync(() -> {
            // 修改点：Feign 直接返回 List 可能为 null，统一通过 Rx 安全取值
            List<OrderDTO> result = Rx.list(api.findByIds(orderSet.stream().collect(Collectors.toList())));
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
    public static CompletableFuture<Map<String, TransportOrderDTO>> tranOrderMapFuture(TransportOrderFeign api, Set<String> orderSet) {
        return CompletableFuture.supplyAsync(() -> {
            // 修改点：Feign 直接返回 List 可能为 null，统一通过 Rx 安全取值
            List<TransportOrderDTO> result = Rx.list(api.findByOrderIds(orderSet.stream().collect(Collectors.toList())));
            return result.stream().collect(Collectors.toMap(TransportOrderDTO::getOrderId, item -> item, (v1, v2) -> v1));
        });
    }
}
