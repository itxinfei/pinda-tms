package com.xinfei.future;

import com.xinfei.common.context.AsyncRequestHeaders;
import com.xinfei.DTO.AreaDTO;
import com.xinfei.DTO.OrderCargoDto;
import com.xinfei.DTO.OrgDTO;
import com.xinfei.DTO.UserDTO;
import com.xinfei.DTO.angency.AgencyScopeDto;
import com.xinfei.DTO.transportline.TransportLineDto;
import com.xinfei.DTO.transportline.TransportTripsDto;
import com.xinfei.DTO.truck.TruckDto;
import com.xinfei.enums.org.OrgType;
import com.xinfei.feign.AreaFeign;
import com.xinfei.feign.CargoFeign;
import com.xinfei.feign.OrgFeign;
import com.xinfei.feign.UserFeign;
import com.xinfei.feign.agency.AgencyScopeFeign;
import com.xinfei.feign.transportline.TransportLineFeign;
import com.xinfei.feign.transportline.TransportTripsFeign;
import com.xinfei.feign.truck.TruckFeign;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
public class PdCompletableFuture {
    /**
     * 获取机构数据列表
     *
     * @param feign      数据接口
     * @param agencyType 机构类型
     * @param ids        机构id列表
     * @return 执行结果
     */
    public static final CompletableFuture<Map<Long, OrgDTO>> agencyMapFuture(OrgFeign feign, Integer agencyType, Set<Long> ids, Long countyId) {
        return AsyncRequestHeaders.supplyAsync(() -> {
            log.info("agencyMapFuture : {} , {} , {}", agencyType, ids, countyId);
            List<Long> idList = new ArrayList<>(ids);
            if (!CollectionUtils.isEmpty(idList)) {
                List<OrgDTO> result = feign.list(agencyType,
                        idList,
                        countyId,
                        null,
                        new ArrayList<>());
                if (result != null) {
                    return result.stream().collect(Collectors.toMap(OrgDTO::getId, org -> org));
                }
            }
            return new HashMap<>();
        });
    }

    public static CompletableFuture<Map<Long, OrgDTO>> businessHallMapFuture(OrgFeign feign, Set<Long> set) {
        return AsyncRequestHeaders.supplyAsync(() -> {
            List<Long> list = new ArrayList<>(set);
            List<OrgDTO> orgs = feign.listByCountyIds(OrgType.BUSINESS_HALL.getType(), list);
            if (orgs == null) {
                return new HashMap<>();
            }
            return orgs.stream().collect(Collectors.toMap(OrgDTO::getCountyId, value -> value));
        });
    }

    public static CompletableFuture<Map<String, String>> agencyScopeMapFuture(AgencyScopeFeign feign, Set<String> set) {
        return AsyncRequestHeaders.supplyAsync(() -> {
            List<String> list = set.stream().collect(Collectors.toList());
            List<AgencyScopeDto> agencyScopeDtos = feign.findAllAgencyScope(null, null, null, list);
            return agencyScopeDtos.stream().collect(Collectors.toMap(AgencyScopeDto::getAreaId, value -> value.getAgencyId()));
        });
    }

    public static CompletableFuture<Map<String, OrderCargoDto>> orderCargoMapFuture(CargoFeign feign, Set<String> set) {
        return AsyncRequestHeaders.supplyAsync(() -> {
            List<String> list = set.stream().collect(Collectors.toList());
            List<OrderCargoDto> orderCargoDtos = feign.list(list);
            return orderCargoDtos.stream().collect(Collectors.toMap(OrderCargoDto::getOrderId, value -> value));
        });
    }

    public static CompletableFuture<Map<String, TransportLineDto>> transportLineIdMapFuture(TransportLineFeign api, Set<String> transportLineIdSet) {
        return AsyncRequestHeaders.supplyAsync(() -> {
            List<String> list = transportLineIdSet.stream().collect(Collectors.toList());
            List<TransportLineDto> result = api.findAll(list, null, null);
            return result.stream().collect(Collectors.toMap(TransportLineDto::getId, item -> item));
        });
    }

    public static CompletableFuture<Map<String, TransportTripsDto>> tripsMapFuture(TransportTripsFeign api, Set<String> tripsIdSet) {
        return AsyncRequestHeaders.supplyAsync(() -> {
            List<String> list = tripsIdSet.stream().collect(Collectors.toList());
            List<TransportTripsDto> result = api.findAll(null, list);
            // 远程调用失败(fallback 返回 null)时返回空 Map，避免 NPE
            if (result == null) {
                return new HashMap<>();
            }
            return result.stream().collect(Collectors.toMap(TransportTripsDto::getId, item -> item));
        });
    }

    public static CompletableFuture<Map<String, TruckDto>> truckMapFuture(TruckFeign api, Set<String> truckIdSet) {
        return AsyncRequestHeaders.supplyAsync(() -> {
            List<String> list = truckIdSet.stream().collect(Collectors.toList());
            List<TruckDto> result = api.findAll(list, null);
            return result.stream().collect(Collectors.toMap(TruckDto::getId, item -> item));
        });
    }

    public static CompletableFuture<Map<Long, UserDTO>> driverMapFuture(UserFeign api, Set<Long> driverIdSet) {
        return AsyncRequestHeaders.supplyAsync(() -> {
            List<Long> list = new ArrayList<>(driverIdSet);

            List<UserDTO> result = api.list(list, null, null, null);
            if (result != null) {
                return result.stream().collect(Collectors.toMap(UserDTO::getId, item -> item));
            }
            return new HashMap<>();
        });
    }

    public static final CompletableFuture<Map<Long, AreaDTO>> areaMapFuture(AreaFeign api, Long parentId, Set<Long> areaSet) {
        // D-02 空集守卫（《12》§8.1）：空集合打 Feign 会让下游 findAll 无 WHERE 条件全表扫描
        if (areaSet == null || areaSet.isEmpty()) {
            return CompletableFuture.completedFuture(new HashMap<>());
        }
        List<AreaDTO> result = api.findAll(parentId, new ArrayList<>(areaSet));
        return AsyncRequestHeaders.supplyAsync(() -> {
                    if (result == null) {
                        return new HashMap<Long, AreaDTO>();
                    }
                    return result.stream().collect(Collectors.toMap(AreaDTO::getId, vo -> vo));
                });
    }
}
