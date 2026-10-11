package com.itheima.pinda.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.itheima.pinda.DTO.*;
import com.itheima.pinda.DTO.transportline.TransportLineDto;
import com.itheima.pinda.DTO.transportline.TransportTripsDto;
import com.itheima.pinda.DTO.truck.TruckDto;
import com.itheima.pinda.common.exception.PdException;
import com.itheima.pinda.common.utils.PageResponse;
import com.itheima.pinda.entity.*;
import com.itheima.pinda.feign.OrgFeign;
import com.itheima.pinda.feign.UserFeign;
import com.itheima.pinda.feign.transportline.TransportLineFeign;
import com.itheima.pinda.feign.transportline.TransportTripsFeign;
import com.itheima.pinda.feign.truck.TruckFeign;
import com.itheima.pinda.future.PdCompletableFuture;
import com.itheima.pinda.service.*;
import com.itheima.pinda.utils.IdConvert;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 定时任务日志
 *
 * @author
 */
@Slf4j
@RestController
@RequestMapping("/scheduleLog")
@Tag(name = "定时任务日志")
public class ScheduleJobLogController {
    @Autowired
    private IScheduleJobLogService scheduleJobLogService;
    @Autowired
    private IScheduleJobService scheduleJobService;

    @Autowired
    private OrgFeign orgFeign;
    @Autowired
    private IOrderClassifyService orderClassifyService;
    @Autowired
    private IOrderClassifyAttachService orderClassifyAttachService;
    @Autowired
    private ICacheLineUseService cacheLineUseService;
    @Autowired
    private ICacheLineService cacheLineService;
    @Autowired
    private ICacheLineDetailService cacheLineDetailService;

    @Autowired
    private TransportLineFeign transportLineFeign;
    @Autowired
    private TransportTripsFeign transportTripsFeign;
    @Autowired
    private UserFeign userFeign;
    @Autowired
    private TruckFeign truckFeign;


    @GetMapping("page")
    @Operation(summary = "分页")
    public PageResponse<ScheduleJobLogDTO> page(@Parameter(hidden = true) @RequestParam Map<String, Object> params) {
        if (!params.containsKey("page")) {
            params.put("page", 1);
        }
        if (!params.containsKey("pageSize")) {
            params.put("pageSize", 10);
        }
        if (!params.containsKey("jobId")) {
            Object orgIdObj = params.get("orgId");
            if (orgIdObj == null) {
                //修改点：PageResponse 为纯数据载体，无 error 方法，统一以 PdException 表达参数错误
                throw new PdException("缺少 orgId 参数");
            }
            Long orgId = IdConvert.toLong(orgIdObj.toString());
            if (orgId == null) {
                throw new PdException("orgId 参数非法");
            }
            ScheduleJobDTO scheduleJobDto = scheduleJobService.getByOrgId(orgId);
            if (scheduleJobDto == null) {
                throw new PdException("该机构无调度任务");
            }
            params.put("jobId", scheduleJobDto.getId());
        }

        IPage<ScheduleJobLogEntity> page = scheduleJobLogService.page(params);

        List<ScheduleJobLogDTO> newItems = page.getRecords().stream().map(item -> {
            ScheduleJobLogDTO scheduleJobLogDTO = new ScheduleJobLogDTO();
            BeanUtils.copyProperties(item, scheduleJobLogDTO);
            return scheduleJobLogDTO;
        }).collect(Collectors.toList());

        return PageResponse.<ScheduleJobLogDTO>builder()
                .page(Integer.valueOf(String.valueOf(page.getCurrent())))
                .pagesize(Integer.valueOf(String.valueOf(page.getSize())))
                .pages(page.getPages())
                .counts(page.getTotal())
                .items(newItems)
                .build();
    }

    @SneakyThrows
    @GetMapping("{id}")
    @Operation(summary = "信息")
    public ScheduleJobLogDTO info(@PathVariable("id") Long id) {
        ScheduleJobLogDTO logDto = scheduleJobLogService.get(id);
        if (logDto == null) {
            return null;
        }
        // 组合 订单分组等信息
        List<OrderClassifyEntity> orderClassifyEntities = orderClassifyService.findByJobLogId(logDto.getId());
        Set<Long> agencySet = new HashSet<>();
        agencySet.addAll(orderClassifyEntities.stream().map(OrderClassifyEntity::getStartOrgId).filter(Objects::nonNull).collect(Collectors.toSet()));
        agencySet.addAll(orderClassifyEntities.stream().map(OrderClassifyEntity::getEndOrgId).filter(Objects::nonNull).collect(Collectors.toSet()));

        CompletableFuture<Map<Long, OrgDTO>> agnecyMapFuture = PdCompletableFuture.agencyMapFuture(orgFeign, null, agencySet, null);
        Map<Long, OrgDTO> agencyMap = agnecyMapFuture.get();

        List<OrderClassifyLogDTO> orderClassifyLogDTOS = orderClassifyEntities.stream().map(item -> {
            OrderClassifyLogDTO orderClassifyLogDTO = new OrderClassifyLogDTO();
            BeanUtils.copyProperties(item, orderClassifyLogDTO);
            if (orderClassifyLogDTO.getStartOrgId() != null) {
                OrgDTO startAgency = agencyMap.get(orderClassifyLogDTO.getStartOrgId());
                if (startAgency != null) {
                    orderClassifyLogDTO.setStartAgency(startAgency.getName());
                }
            }
            if (orderClassifyLogDTO.getEndOrgId() != null) {
                OrgDTO endAgency = agencyMap.get(orderClassifyLogDTO.getEndOrgId());
                if (endAgency != null) {
                    orderClassifyLogDTO.setEndAgency(endAgency.getName());
                }
            }

            buildTransportLine(item, orderClassifyLogDTO);

            buildTripsTruckDriver(item, orderClassifyLogDTO);

            return orderClassifyLogDTO;
        }).collect(Collectors.toList());

        logDto.setOrderClassifyLogDTOS(orderClassifyLogDTOS);
        return logDto;
    }

    /**
     * 获取当前使用的车次 车辆 司机信息
     *
     * @param item
     * @param orderClassifyLogDTO
     */
    @SneakyThrows
    private void buildTripsTruckDriver(OrderClassifyEntity item, OrderClassifyLogDTO orderClassifyLogDTO) {
        LambdaQueryWrapper<OrderClassifyAttachEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderClassifyAttachEntity::getClassifyId, item.getId());
        wrapper.orderByAsc(OrderClassifyAttachEntity::getCreateTime);
        List<OrderClassifyAttachEntity> orderClassifyAttachs = orderClassifyAttachService.list(wrapper);

        // 车次/车辆为 pd-base String 边界，转 String 集合后远程查询；司机内部即 Long
        Set<String> tripsIdSet = IdConvert.toStrSet(orderClassifyAttachs.stream().map(OrderClassifyAttachEntity::getTripsId).collect(Collectors.toSet()));
        Set<String> truckIdSet = IdConvert.toStrSet(orderClassifyAttachs.stream().map(OrderClassifyAttachEntity::getTruckId).collect(Collectors.toSet()));
        Set<Long> driverIdSet = orderClassifyAttachs.stream().map(OrderClassifyAttachEntity::getDriverId).collect(Collectors.toSet());

        CompletableFuture<Map<String, TransportTripsDto>> tripsMapFuture = PdCompletableFuture.tripsMapFuture(transportTripsFeign, tripsIdSet);
        CompletableFuture<Map<String, TruckDto>> truckMapFuture = PdCompletableFuture.truckMapFuture(truckFeign, truckIdSet);
        CompletableFuture<Map<Long, UserDTO>> driverMapFuture = PdCompletableFuture.driverMapFuture(userFeign, driverIdSet);

        Set<String> tripsSet = new LinkedHashSet<>();
        Set<String> truckSet = new LinkedHashSet<>();
        Set<String> driverSet = new LinkedHashSet<>();
        Map<String, TransportTripsDto> tripsMap = tripsMapFuture.get();
        Map<String, TruckDto> truckMap = truckMapFuture.get();
        Map<Long, UserDTO> driverMap = driverMapFuture.get();
        for (OrderClassifyAttachEntity orderClassifyAttach : orderClassifyAttachs) {
            tripsSet.add(tripsMap.get(IdConvert.toStr(orderClassifyAttach.getTripsId())).getName());
            truckSet.add(truckMap.get(IdConvert.toStr(orderClassifyAttach.getTruckId())).getLicensePlate());
            driverSet.add(driverMap.get(orderClassifyAttach.getDriverId()).getName());
        }
        orderClassifyLogDTO.setTripsSet(tripsSet);
        orderClassifyLogDTO.setTruckSet(truckSet);
        orderClassifyLogDTO.setDriverSet(driverSet);
    }

    /**
     * 构建当前分类的订单经过的全部机构 和 全部的线路
     *
     * @param item
     * @param orderClassifyLogDTO
     */
    @SneakyThrows
    private void buildTransportLine(OrderClassifyEntity item, OrderClassifyLogDTO orderClassifyLogDTO) {
        CacheLineUseEntity cacheLineUseEntity = cacheLineUseService.getByClassifyId(item.getId());
        Set<String> lineSet = new LinkedHashSet<>();
        Set<String> transportLineSet = new LinkedHashSet<>();
        if (cacheLineUseEntity != null) {
            CacheLineEntity cacheLineEntity = cacheLineService.getById(cacheLineUseEntity.getCacheLineId());
            List<CacheLineDetailEntity> cacheLineDetailEntities = cacheLineDetailService.findByCacheLineId(cacheLineUseEntity.getCacheLineId());
            // 线路ID为 pd-base String 边界，转 String 集合后远程查询
            Set<String> transportLineIdSet = IdConvert.toStrSet(cacheLineDetailEntities.stream().map(CacheLineDetailEntity::getTransportLineId).collect(Collectors.toSet()));
            CompletableFuture<Map<String, TransportLineDto>> transportLineMapFuture = PdCompletableFuture.transportLineIdMapFuture(transportLineFeign, transportLineIdSet);

            Set<Long> agencySet = new HashSet<>();
            agencySet.add(cacheLineEntity.getStartOrgId());
            agencySet.add(cacheLineEntity.getEndOrgId());
            agencySet.addAll(cacheLineDetailEntities.stream().map(CacheLineDetailEntity::getStartOrgId).collect(Collectors.toSet()));
            agencySet.addAll(cacheLineDetailEntities.stream().map(CacheLineDetailEntity::getEndOrgId).collect(Collectors.toSet()));

            CompletableFuture<Map<Long, OrgDTO>> agnecyMapFu = PdCompletableFuture.agencyMapFuture(orgFeign, null, agencySet, null);
            Map<String, TransportLineDto> transportLineMap = transportLineMapFuture.get();
            Map<Long, OrgDTO> agency = agnecyMapFu.get();

            cacheLineDetailEntities.forEach(cacheLineDetailEntity -> {
                CacheLineDetailDTO cacheLineDetailDTO = new CacheLineDetailDTO();
                BeanUtils.copyProperties(cacheLineDetailEntity, cacheLineDetailDTO);
                if (cacheLineDetailDTO.getStartOrgId() != null) {
                    cacheLineDetailDTO.setStartAgency(agency.get(cacheLineDetailDTO.getStartOrgId()).getName());
                }
                if (cacheLineDetailDTO.getEndOrgId() != null) {
                    cacheLineDetailDTO.setEndAgency(agency.get(cacheLineDetailDTO.getEndOrgId()).getName());
                }
                transportLineSet.add(transportLineMap.get(IdConvert.toStr(cacheLineDetailDTO.getTransportLineId())).getName());

                lineSet.add(cacheLineDetailDTO.getStartAgency());
                lineSet.add(cacheLineDetailDTO.getEndAgency());

            });

            orderClassifyLogDTO.setTransportLineSet(transportLineSet);
            orderClassifyLogDTO.setLineSet(lineSet);


            CacheLineDTO cacheLineDTO = new CacheLineDTO();
            BeanUtils.copyProperties(cacheLineEntity, cacheLineDTO);
            orderClassifyLogDTO.setCacheLineDTO(cacheLineDTO);
        }
    }
}
