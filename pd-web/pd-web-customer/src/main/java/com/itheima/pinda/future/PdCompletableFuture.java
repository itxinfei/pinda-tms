package com.itheima.pinda.future;

import com.itheima.pinda.DTO.AreaDTO;
import com.itheima.pinda.DTO.OrderCargoDto;
import com.itheima.pinda.DTO.OrgDTO;
import com.itheima.pinda.DTO.TaskPickupDispatchDTO;
import com.itheima.pinda.DTO.TransportOrderDTO;
import com.itheima.pinda.enums.pickuptask.PickupDispatchTaskStatus;
import com.itheima.pinda.feign.AreaFeign;
import com.itheima.pinda.feign.CargoFeign;
import com.itheima.pinda.feign.OrgFeign;
import com.itheima.pinda.feign.PickupDispatchTaskFeign;
import com.itheima.pinda.feign.TransportOrderFeign;
import com.itheima.pinda.util.Rx;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
public class PdCompletableFuture {


    public static final CompletableFuture<Map<Long, AreaDTO>> areaMapFuture(AreaFeign api, Long parentId, Set<Long> areaSet) {
        // Feign 直接返回裸 List（无 R 包装），远程失败可能为 null，统一通过 Rx 安全取值
        List<AreaDTO> result = api.findAll(parentId, new ArrayList<>(areaSet));
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
    public static final CompletableFuture<Map<Long, OrgDTO>> agencyMapFuture(OrgFeign api, Integer agencyType, Set<String> ids, Long countyId) {
        return CompletableFuture.supplyAsync(() -> {
            List<OrgDTO> result = api.list(agencyType, ids.stream().mapToLong(Long::valueOf).boxed().collect(Collectors.toList()), countyId, null, null);
            // 远程调用成功但结果可能为 null，增加判空避免 NPE
            if (result != null) {
                return result.stream().collect(Collectors.toMap(OrgDTO::getId, org -> org));
            }
            return new HashMap<>();
        });
    }

    /**
     * 获取货物信息列表
     *
     * @param api
     * @param cargoSet
     * @return
     */
    public static CompletableFuture<Map<String, OrderCargoDto>> cargoMapFuture(CargoFeign api, Set<String> cargoSet) {
        return CompletableFuture.supplyAsync(() -> {
            if (CollectionUtils.isEmpty(cargoSet)) {
                return new HashMap<>();
            }
            List<OrderCargoDto> result = api.list(cargoSet.stream().collect(Collectors.toList()));
            if (!CollectionUtils.isEmpty(result)) {
                return result.stream().collect(Collectors.toMap(OrderCargoDto::getOrderId, cargoDto -> cargoDto));
            }
            return new HashMap<>();
        });
    }


    /**
     * @param api
     * @param taskPickupDispatchSet
     * @param taskType
     * @return
     */
    public static CompletableFuture<Map<String, TaskPickupDispatchDTO>> taskTranSportMapFuture(PickupDispatchTaskFeign api, Set<String> taskPickupDispatchSet, Integer taskType) {
        return CompletableFuture.supplyAsync(() -> {
            TaskPickupDispatchDTO queryDTO = new TaskPickupDispatchDTO();
            queryDTO.setTaskType(taskType);
            // TaskPickupDispatchDTO.orderIds 已 Long 化（订单 ID），调用边界转换
            queryDTO.setOrderIds(taskPickupDispatchSet.stream().map(Long::valueOf).collect(Collectors.toList()));
            List<TaskPickupDispatchDTO> result = api.findAll(queryDTO);
            if (!CollectionUtils.isEmpty(result)) {
                log.info("TaskPickupDispatchDTO result：{}", result);
                result = result.stream().filter(item -> (!PickupDispatchTaskStatus.CANCELLED.getCode().equals(item.getStatus()))).collect(Collectors.toList());
                log.info("TaskPickupDispatchDTO result by duplicate：{}", result);
                // Map 以订单 ID(String) 为键供调用方查询；orderId 为 null 无法入键，先过滤
                return result.stream()
                        .filter(item -> item.getOrderId() != null)
                        .collect(Collectors.toMap(item -> String.valueOf(item.getOrderId()), item -> item, (v1, v2) -> v1));
            }
            return new HashMap<>();
        });
    }

    public static CompletableFuture<Map<String, TransportOrderDTO>> transportOrderMapFuture(TransportOrderFeign api, Set<String> taskPickupDispatchSet) {
        return CompletableFuture.supplyAsync(() -> {
            if (CollectionUtils.isEmpty(taskPickupDispatchSet)) {
                return new HashMap<>();
            }
            List<TransportOrderDTO> result = api.findByOrderIds(taskPickupDispatchSet.stream().map(Long::valueOf).collect(Collectors.toList()));
            if (!CollectionUtils.isEmpty(result)) {
                return result.stream()
                        .filter(dto -> dto.getOrderId() != null)
                        .collect(Collectors.toMap(dto -> String.valueOf(dto.getOrderId()), transportOrderDTO -> transportOrderDTO));
            }
            return new HashMap<>();
        });
    }

}
