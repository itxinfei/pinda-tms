package com.xinfei.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;

import com.xinfei.DTO.AreaDTO;
import com.xinfei.DTO.DriverJobDTO;
import com.xinfei.DTO.OrderDTO;
import com.xinfei.DTO.OrgDTO;
import com.xinfei.DTO.TaskTransportDTO;
import com.xinfei.DTO.TransportOrderDTO;
import com.xinfei.DTO.UserDTO;
import com.xinfei.DTO.webManager.DriverJobQueryDTO;
import com.xinfei.DTO.transportline.TransportTripsDto;
import com.xinfei.DTO.truck.TruckDto;
import com.xinfei.common.utils.PageResponse;
import com.xinfei.feign.AreaFeign;
import com.xinfei.feign.DriverJobFeign;
import com.xinfei.feign.OrderFeign;
import com.xinfei.feign.OrgFeign;
import com.xinfei.feign.TransportOrderFeign;
import com.xinfei.feign.TransportTaskFeign;
import com.xinfei.feign.UserFeign;
import com.xinfei.feign.transportline.TransportTripsFeign;
import com.xinfei.feign.truck.TruckFeign;
import com.xinfei.feign.webManager.WebManagerFeign;
import com.xinfei.util.BeanUtil;
import com.xinfei.util.EnrichContext;
import com.xinfei.util.Rx;
import com.xinfei.vo.base.userCenter.SysUserVo;
import com.xinfei.vo.work.DriverJobVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 司机作业单相关API
 *
 * @author itcast
 */
@RestController
@Slf4j
@Tag(name = "司机作业单相关API")
@RequestMapping("driver-job-manager")
public class DriverJobController {
    @Autowired
    private WebManagerFeign webManagerFeign;
    @Autowired
    private TransportTaskFeign transportTaskFeign;
    @Autowired
    private TransportTripsFeign transportTripsFeign;
    @Autowired
    private OrgFeign orgFeign;
    @Autowired
    private UserFeign userFeign;
    @Autowired
    private TruckFeign truckFeign;
    @Autowired
    private TransportOrderFeign transportOrderFeign;
    @Autowired
    private OrderFeign orderFeign;
    @Autowired
    private AreaFeign areaFeign;
    @Autowired
    private DriverJobFeign driverJobFeign;

    @Operation(summary = "获取司机作业单分页数据")
    @PostMapping("/page")
    @SaCheckPermission("driverJob:list")
    public PageResponse<DriverJobVo> findByPage(@RequestBody DriverJobVo vo) {
        DriverJobQueryDTO dto = new DriverJobQueryDTO();
        if (vo != null) {
            dto.setPage(vo.getPage());
            dto.setPageSize(vo.getPageSize());
            if (vo.getDriver() != null) {
                dto.setDriverName(vo.getDriver().getName());
            }
            if (vo.getTaskTransport() != null) {
                dto.setTaskTransportId(vo.getTaskTransport().getId());
            }
            dto.setStatus(vo.getStatus());
            dto.setId(vo.getId());
        }
        PageResponse<DriverJobDTO> dtoPageResponse = webManagerFeign.findDriverJobByPage(dto);
        // 远程调用返回 PageResponse 可能为 null，先判空避免 NPE
        if (dtoPageResponse == null) {
            return PageResponse.<DriverJobVo>builder().items(new ArrayList<>()).pagesize(vo.getPageSize()).page(vo.getPage()).counts(0L).pages(0L).build();
        }
        List<DriverJobDTO> dtoList = dtoPageResponse.getItems();
        if (CollectionUtils.isEmpty(dtoList)) {
            return PageResponse.<DriverJobVo>builder().items(new ArrayList<>()).pagesize(vo.getPageSize()).page(vo.getPage()).counts(0L).pages(0L).build();
        }

        // ===== 批量预取，消除三重嵌套 N+1（巡检报告 P0-1）=====
        // 一页内原本逐行（org×2 + user + 嵌套运输任务的 trips/org×2/truck/运单×N(订单+6区划)/司机）
        // 可达约 490 次远程调用；改为整页一次批量预取后压到约 30 次。
        Set<Long> orgIds = new HashSet<>();
        Set<Long> userIds = new HashSet<>();
        List<Long> taskTransportIds = new ArrayList<>();
        for (DriverJobDTO dj : dtoList) {
            addIfPresent(orgIds, dj.getStartOrgId());
            addIfPresent(orgIds, dj.getEndOrgId());
            addIfPresent(userIds, dj.getDriverId());
            addIfPresent(taskTransportIds, dj.getTaskTransportId());
        }
        Map<Long, OrgDTO> orgMap = batchOrg(orgIds);
        Map<Long, UserDTO> userMap = batchUser(userIds);

        // 运输任务无批量端点，逐条取（1 次/行，非 N²），用于收集内部富化所需的 id
        Map<Long, TaskTransportDTO> ttMap = new HashMap<>();
        if (transportTaskFeign != null) {
            for (Long ttId : taskTransportIds) {
                if (ttId == null) {
                    continue;
                }
                TaskTransportDTO tt = transportTaskFeign.findById(ttId);
                if (tt != null) {
                    ttMap.put(ttId, tt);
                }
            }
        }
        Set<Long> tripsIds = new HashSet<>();
        Set<Long> truckIds = new HashSet<>();
        List<Long> transportOrderIds = new ArrayList<>();
        for (TaskTransportDTO tt : ttMap.values()) {
            addIfPresent(tripsIds, tt.getTripsId());
            addIfPresent(truckIds, tt.getTruckId());
            if (tt.getTransportOrderIds() != null) {
                transportOrderIds.addAll(tt.getTransportOrderIds());
            }
        }
        Map<Long, TransportOrderDTO> transportOrderMap = batchTransportOrder(transportOrderIds);
        Set<Long> orderIds = new HashSet<>();
        for (TransportOrderDTO to : transportOrderMap.values()) {
            addIfPresent(orderIds, to.getOrderId());
        }
        Map<String, OrderDTO> orderMap = batchOrder(orderIds);
        Set<Long> areaIds = new HashSet<>();
        for (OrderDTO o : orderMap.values()) {
            collectOrderAreaIds(areaIds, o);
        }
        Map<Long, AreaDTO> areaMap = batchArea(areaIds);
        Map<String, TransportTripsDto> tripsMap = batchTrips(tripsIds);
        Map<String, TruckDto> truckMap = batchTruck(truckIds);
        Map<Long, List<SysUserVo>> driversByTaskId = BeanUtil.parseTaskDriversBatch(taskTransportIds, driverJobFeign, userFeign);

        EnrichContext ctx = EnrichContext.builder(transportTripsFeign, orgFeign, userFeign, truckFeign,
                        transportOrderFeign, orderFeign, areaFeign, driverJobFeign)
                .orgMap(orgMap).userMap(userMap).tripsMap(tripsMap).truckMap(truckMap)
                .transportOrderMap(transportOrderMap).orderMap(orderMap).areaMap(areaMap)
                .driversByTaskId(driversByTaskId).build();

        List<DriverJobVo> voList = dtoList.stream().map(dj -> {
            TaskTransportDTO tt = dj.getTaskTransportId() != null ? ttMap.get(dj.getTaskTransportId()) : null;
            return BeanUtil.parseDriverJobDTO2Vo(dj, tt, ctx);
        }).collect(Collectors.toList());

        return PageResponse.<DriverJobVo>builder().items(voList).pagesize(vo.getPageSize()).page(vo.getPage()).counts(dtoPageResponse.getCounts()).pages(dtoPageResponse.getPages()).build();
    }

    // ---- 批量预取辅助（null 安全，依赖 Rx.list 兜底空集合）----

    private static void addIfPresent(Collection<Long> c, Long v) {
        if (v != null) {
            c.add(v);
        }
    }

    private static void collectOrderAreaIds(Set<Long> ids, OrderDTO o) {
        if (o == null) {
            return;
        }
        addIfPresent(ids, parseLong(o.getSenderProvinceId()));
        addIfPresent(ids, parseLong(o.getSenderCityId()));
        addIfPresent(ids, parseLong(o.getSenderCountyId()));
        addIfPresent(ids, parseLong(o.getReceiverProvinceId()));
        addIfPresent(ids, parseLong(o.getReceiverCityId()));
        addIfPresent(ids, parseLong(o.getReceiverCountyId()));
    }

    private static Long parseLong(String s) {
        if (s == null) {
            return null;
        }
        try {
            return Long.valueOf(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Map<Long, OrgDTO> batchOrg(Set<Long> ids) {
        Map<Long, OrgDTO> map = new LinkedHashMap<>();
        if (ids.isEmpty() || orgFeign == null) {
            return map;
        }
        for (OrgDTO o : Rx.list(orgFeign.list(null, new ArrayList<>(ids), null, null, null))) {
            if (o != null && o.getId() != null) {
                map.put(o.getId(), o);
            }
        }
        return map;
    }

    private Map<Long, UserDTO> batchUser(Set<Long> ids) {
        Map<Long, UserDTO> map = new LinkedHashMap<>();
        if (ids.isEmpty() || userFeign == null) {
            return map;
        }
        for (UserDTO u : Rx.list(userFeign.list(new ArrayList<>(ids), null, null, null))) {
            if (u != null && u.getId() != null) {
                map.put(u.getId(), u);
            }
        }
        return map;
    }

    private Map<Long, TransportOrderDTO> batchTransportOrder(List<Long> ids) {
        Map<Long, TransportOrderDTO> map = new LinkedHashMap<>();
        if (ids.isEmpty() || transportOrderFeign == null) {
            return map;
        }
        for (TransportOrderDTO to : Rx.list(transportOrderFeign.findByOrderIds(ids))) {
            if (to != null && to.getId() != null) {
                map.put(to.getId(), to);
            }
        }
        return map;
    }

    private Map<String, OrderDTO> batchOrder(Set<Long> ids) {
        Map<String, OrderDTO> map = new LinkedHashMap<>();
        if (ids.isEmpty() || orderFeign == null) {
            return map;
        }
        List<String> strIds = ids.stream().map(String::valueOf).collect(Collectors.toList());
        for (OrderDTO o : Rx.list(orderFeign.findByIds(strIds))) {
            if (o != null && o.getId() != null) {
                map.put(o.getId(), o);
            }
        }
        return map;
    }

    private Map<Long, AreaDTO> batchArea(Set<Long> ids) {
        Map<Long, AreaDTO> map = new LinkedHashMap<>();
        if (ids.isEmpty() || areaFeign == null) {
            return map;
        }
        for (AreaDTO a : Rx.list(areaFeign.findAll(null, new ArrayList<>(ids)))) {
            if (a != null && a.getId() != null) {
                map.put(a.getId(), a);
            }
        }
        return map;
    }

    private Map<String, TransportTripsDto> batchTrips(Set<Long> ids) {
        Map<String, TransportTripsDto> map = new LinkedHashMap<>();
        if (ids.isEmpty() || transportTripsFeign == null) {
            return map;
        }
        List<String> strIds = ids.stream().map(String::valueOf).collect(Collectors.toList());
        for (TransportTripsDto t : Rx.list(transportTripsFeign.findAll(null, strIds))) {
            if (t != null && t.getId() != null) {
                map.put(t.getId(), t);
            }
        }
        return map;
    }

    private Map<String, TruckDto> batchTruck(Set<Long> ids) {
        Map<String, TruckDto> map = new LinkedHashMap<>();
        if (ids.isEmpty() || truckFeign == null) {
            return map;
        }
        List<String> strIds = ids.stream().map(String::valueOf).collect(Collectors.toList());
        for (TruckDto t : Rx.list(truckFeign.findAll(strIds, null))) {
            if (t != null && t.getId() != null) {
                map.put(t.getId(), t);
            }
        }
        return map;
    }
}
