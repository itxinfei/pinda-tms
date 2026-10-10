package com.itheima.pinda.future;

import com.itheima.pinda.DTO.AreaDTO;
import com.itheima.pinda.DTO.OrderCargoDto;
import com.itheima.pinda.DTO.OrgDTO;
import com.itheima.pinda.DTO.UserDTO;
import com.itheima.pinda.DTO.angency.AgencyScopeDto;
import com.itheima.pinda.DTO.transportline.TransportLineDto;
import com.itheima.pinda.DTO.transportline.TransportTripsDto;
import com.itheima.pinda.DTO.truck.TruckDto;
import com.itheima.pinda.enums.org.OrgType;
import com.itheima.pinda.feign.AreaFeign;
import com.itheima.pinda.feign.CargoFeign;
import com.itheima.pinda.feign.OrgFeign;
import com.itheima.pinda.feign.UserFeign;
import com.itheima.pinda.feign.agency.AgencyScopeFeign;
import com.itheima.pinda.feign.transportline.TransportLineFeign;
import com.itheima.pinda.feign.transportline.TransportTripsFeign;
import com.itheima.pinda.feign.truck.TruckFeign;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
    public static final CompletableFuture<Map<Long, OrgDTO>> agencyMapFuture(OrgFeign feign, Integer agencyType, Set<String> ids, Long countyId) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("agencyMapFuture : {} , {} , {}", agencyType, ids, countyId);
            List<Long> idList = ids.stream().filter(StringUtils::isNotBlank).mapToLong(Long::valueOf).boxed().collect(Collectors.toList());
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

    public static CompletableFuture<Map<Long, OrgDTO>> businessHallMapFuture(OrgFeign feign, Set<String> set) {
        return CompletableFuture.supplyAsync(() -> {
            List<Long> list = set.stream().map(Long::parseLong).collect(Collectors.toList());
            List<OrgDTO> orgs = feign.listByCountyIds(OrgType.BUSINESS_HALL.getType(), list);
            if (orgs == null) {
                return new HashMap<>();
            }
            return orgs.stream().collect(Collectors.toMap(OrgDTO::getCountyId, value -> value));
        });
    }

    public static CompletableFuture<Map<String, String>> agencyScopeMapFuture(AgencyScopeFeign feign, Set<String> set) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> list = set.stream().collect(Collectors.toList());
            List<AgencyScopeDto> agencyScopeDtos = feign.findAllAgencyScope(null, null, null, list);
            return agencyScopeDtos.stream().collect(Collectors.toMap(AgencyScopeDto::getAreaId, value -> value.getAgencyId()));
        });
    }

    public static CompletableFuture<Map<String, OrderCargoDto>> orderCargoMapFuture(CargoFeign feign, Set<String> set) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> list = set.stream().collect(Collectors.toList());
            List<OrderCargoDto> orderCargoDtos = feign.list(list);
            return orderCargoDtos.stream().collect(Collectors.toMap(OrderCargoDto::getOrderId, value -> value));
        });
    }

    public static CompletableFuture<Map<String, TransportLineDto>> transportLineIdMapFuture(TransportLineFeign api, Set<String> transportLineIdSet) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> list = transportLineIdSet.stream().collect(Collectors.toList());
            List<TransportLineDto> result = api.findAll(list, null, null);
            return result.stream().collect(Collectors.toMap(TransportLineDto::getId, item -> item));
        });
    }

    public static CompletableFuture<Map<String, TransportTripsDto>> tripsMapFuture(TransportTripsFeign api, Set<String> tripsIdSet) {
        return CompletableFuture.supplyAsync(() -> {
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
        return CompletableFuture.supplyAsync(() -> {
            List<String> list = truckIdSet.stream().collect(Collectors.toList());
            List<TruckDto> result = api.findAll(list, null);
            return result.stream().collect(Collectors.toMap(TruckDto::getId, item -> item));
        });
    }

    public static CompletableFuture<Map<Long, UserDTO>> driverMapFuture(UserFeign api, Set<String> driverIdSet) {
        return CompletableFuture.supplyAsync(() -> {
            List<Long> list = driverIdSet.stream().filter(StringUtils::isNotBlank).map(Long::parseLong).collect(Collectors.toList());

            List<UserDTO> result = api.list(list, null, null, null);
            if (result != null) {
                return result.stream().collect(Collectors.toMap(UserDTO::getId, item -> item));
            }
            return new HashMap<>();
        });
    }

    public static final CompletableFuture<Map<Long, AreaDTO>> areaMapFuture(AreaFeign api, Long parentId, Set<Long> areaSet) {
        List<AreaDTO> result = api.findAll(parentId, new ArrayList<>(areaSet));
        return CompletableFuture.supplyAsync(() -> {
                    if (result == null) {
                        return new HashMap<Long, AreaDTO>();
                    }
                    return result.stream().collect(Collectors.toMap(AreaDTO::getId, vo -> vo));
                });
    }
}
