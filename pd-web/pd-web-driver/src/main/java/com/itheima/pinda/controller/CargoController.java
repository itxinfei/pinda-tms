package com.itheima.pinda.controller;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.itheima.pinda.DTO.*;
import com.itheima.pinda.DTO.angency.AgencyScopeDto;
import com.itheima.pinda.DTO.user.CourierScopeDto;
import com.itheima.pinda.constant.StaticStation;
import com.itheima.pinda.common.context.RequestContext;
import com.itheima.pinda.common.enums.ErrorCode;
import com.itheima.pinda.common.exception.PdException;
import com.itheima.pinda.common.utils.EntCoordSyncJob;
import com.itheima.pinda.common.utils.OwnershipAssert;
import com.itheima.pinda.common.utils.PageResponse;
import com.itheima.pinda.common.utils.Result;
import com.itheima.pinda.util.Rx;
import com.itheima.pinda.enums.OrderStatus;
import com.itheima.pinda.enums.driverjob.DriverJobStatus;
import com.itheima.pinda.enums.pickuptask.PickupDispatchTaskAssignedStatus;
import com.itheima.pinda.enums.pickuptask.PickupDispatchTaskStatus;
import com.itheima.pinda.enums.pickuptask.PickupDispatchTaskType;
import com.itheima.pinda.enums.transportorder.TransportOrderStatus;
import com.itheima.pinda.enums.transporttask.TransportTaskStatus;
import com.itheima.pinda.DTO.DriverExceptionReportDTO;
import com.itheima.pinda.feign.*;
import com.itheima.pinda.feign.DriverExceptionReportFeign;
import com.itheima.pinda.feign.agency.AgencyScopeFeign;
import com.itheima.pinda.feign.transportline.TransportTripsFeign;
import com.itheima.pinda.feign.user.CourierScopeFeign;
import com.itheima.pinda.future.PdCompletableFuture;
import com.itheima.pinda.vo.AgencyVo;
import com.itheima.pinda.vo.AreaSimpleVo;
import com.itheima.pinda.vo.SysUserVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * <p>
 * 运单表 前端控制器
 * </p>
 *
 * @author diesel
 * @since 2020-03-19
 */
@Slf4j
@Tag(name = "司机作业单")
@Controller
@RequestMapping("business/cargo")
public class CargoController {


    private final DriverJobFeign driverJobFeign;

    private final OrgFeign orgFeign;

    private final AreaFeign areaFeign;

    private final TransportTripsFeign transportTripsFeign;

    private final UserFeign userFeign;

    private final TransportOrderFeign transportOrderFeign;

    private final TransportTaskFeign transportTaskFeign;

    private final OrderFeign orderFeign;

    private final AgencyScopeFeign agencyScopeFeign;

    private final PickupDispatchTaskFeign pickupDispatchTaskFeign;

    private final CourierScopeFeign courierScopeFeign;

    private final DriverExceptionReportFeign driverExceptionReportFeign;

    public CargoController(PickupDispatchTaskFeign pickupDispatchTaskFeign, AgencyScopeFeign agencyScopeFeign, OrderFeign orderFeign, TransportTaskFeign transportTaskFeign, DriverJobFeign driverJobFeign, OrgFeign orgFeign, AreaFeign areaFeign, TransportOrderFeign transportOrderFeign, TransportTripsFeign transportTripsFeign, UserFeign userFeign,CourierScopeFeign courierScopeFeign, DriverExceptionReportFeign driverExceptionReportFeign) {
        this.pickupDispatchTaskFeign = pickupDispatchTaskFeign;
        this.agencyScopeFeign = agencyScopeFeign;
        this.orderFeign = orderFeign;
        this.transportTaskFeign = transportTaskFeign;
        this.driverJobFeign = driverJobFeign;
        this.orgFeign = orgFeign;
        this.areaFeign = areaFeign;
        this.transportOrderFeign = transportOrderFeign;
        this.transportTripsFeign = transportTripsFeign;
        this.userFeign = userFeign;
        this.courierScopeFeign = courierScopeFeign;
        this.driverExceptionReportFeign = driverExceptionReportFeign;
    }

    @SneakyThrows
    @Operation(summary = "获取待提货列表")
    @Parameters({
            @Parameter(name = "page", description = "当前页数", required = true),
            @Parameter(name = "pagesize", description = "每夜个数", required = true)
    })
    @ResponseBody
    @GetMapping("wait")
    public Result waitDelivery(Integer page, Integer pagesize) {

        //  获取司机id 并放入参数
        String driverId = RequestContext.getUserId();
        log.info("待提货列表:{}", driverId);

        DriverJobDTO driverJobDTO = new DriverJobDTO();
        driverJobDTO.setPage(page);
        driverJobDTO.setPageSize(pagesize);
        driverJobDTO.setStatus(DriverJobStatus.PENDING.getCode());
        driverJobDTO.setDriverId(Long.valueOf(driverId));

        log.info("待提货列表列表 PARAMS:{}", driverJobDTO);
        // 修改点：远程调用返回 PageResponse 可能为 null，统一通过 Rx 安全取值，避免 NPE
        PageResponse<DriverJobDTO> result = driverJobFeign.findByPage(driverJobDTO);
        List<DriverJobDTO> items = Rx.items(result);
        log.info("待提货列表列表 RESULT:{}", items);
        if(items.size()>0){
// 查询地址
            Set<Long> agencySet = new HashSet<>();
            agencySet.addAll(items.stream().map(item -> item.getStartOrgId()).collect(Collectors.toSet()));
            agencySet.addAll(items.stream().map(item -> item.getEndOrgId()).collect(Collectors.toSet()));
            CompletableFuture<List<OrgDTO>> agencyListFuture = PdCompletableFuture.agencyListFuture(orgFeign, null, agencySet, null);
            List<OrgDTO> agencyList = agencyListFuture.get();

            // 查询地区
            Set<Long> areaSet = new HashSet<>();
            areaSet.addAll(agencyList.stream().map(item -> item.getProvinceId()).collect(Collectors.toSet()));
            areaSet.addAll(agencyList.stream().map(item -> item.getCityId()).collect(Collectors.toSet()));
            areaSet.addAll(agencyList.stream().map(item -> item.getCountyId()).collect(Collectors.toSet()));

            CompletableFuture<Map> areaMapFuture = PdCompletableFuture.areaMapFuture(areaFeign, null, areaSet);

            Set<Long> taskTransportSet = items.stream().map(item -> item.getTaskTransportId()).collect(Collectors.toSet());
            CompletableFuture<Map<Long, TaskTransportDTO>> taskTransportFuture = PdCompletableFuture.taskTramsportMapFuture(transportTaskFeign, taskTransportSet);

            Map<Long, TaskTransportDTO> taskTransportMap = taskTransportFuture.get();
            Map areaMap = areaMapFuture.get();

            Map<String, AgencyVo> agencyMap = agencyList.stream().map(item -> {
                AgencyVo agencyVo = new AgencyVo();
                BeanUtils.copyProperties(item, agencyVo);
                agencyVo.setId(item.getId().toString());
                agencyVo.setProvince((AreaSimpleVo) areaMap.get(item.getProvinceId()));
                agencyVo.setCity((AreaSimpleVo) areaMap.get(item.getCityId()));
                agencyVo.setCounty((AreaSimpleVo) areaMap.get(item.getCountyId()));
                return agencyVo;
            }).collect(Collectors.toMap(AgencyVo::getId, vo -> vo));


            List<CargoTranTaskDTO> cargoTranTaskDTOS = items.stream().map(item -> new CargoTranTaskDTO(item, taskTransportMap, agencyMap)).collect(Collectors.toList());

            log.info("待提货列表,转换后的数据：{}", cargoTranTaskDTOS);
            if (CollectionUtils.isEmpty(cargoTranTaskDTOS)) {
                return Result.ok().put("data", PageResponse.<CargoTranTaskDTO>builder()
                        .counts(0L).page(page).pagesize(pagesize).pages(0L)
                        .build());
            }


            driverJobDTO.setStatus(DriverJobStatus.PROCESSING.getCode());

            // 校验是否有在途任务
            PageResponse<DriverJobDTO> resultForProcessing = driverJobFeign.findByPage(driverJobDTO);
            if (resultForProcessing == null || CollectionUtils.isEmpty(resultForProcessing.getItems())) {
                // 没有在途任务  第一个置位显示
                if (page == 1) { // 第一页需要设置按钮显示
                    cargoTranTaskDTOS.get(0).setDisable(true);
                }
            }


            log.info("待提货列表结束:{}", cargoTranTaskDTOS);

            return Result.ok().put("data", PageResponse.<CargoTranTaskDTO>builder()
                    .counts(result.getCounts()).page(page).pagesize(pagesize).pages(result.getPages())
                    .items(cargoTranTaskDTOS).build());
        }else{
            return Result.ok().put("data", PageResponse.<CargoTranTaskDTO>builder()
                    .counts(0L).page(page).pagesize(pagesize).pages(0L)
                    .items(Lists.newArrayList()).build());
        }

    }

    @SneakyThrows
    @Operation(summary = "历史列表")
    @Parameters({
            @Parameter(name = "page", description = "当前页数", required = true),
            @Parameter(name = "pagesize", description = "每夜个数", required = true),
            @Parameter(name = "keyword", description = "搜索条件", required = false)
    })
    @ResponseBody
    @GetMapping("history")
    public Result history(Integer page, Integer pagesize, String keyword) {

        //  获取司机id  并放入参数
        String driverId = RequestContext.getUserId();

        DriverJobDTO driverJobDTO = new DriverJobDTO();
        driverJobDTO.setPage(page);
        driverJobDTO.setPageSize(pagesize);
        driverJobDTO.setStatus(DriverJobStatus.COMPLETED.getCode());
        driverJobDTO.setDriverId(Long.valueOf(driverId));
        driverJobDTO.setId(StringUtils.isBlank(keyword) ? null : Long.valueOf(keyword));

        log.info("历史列表 PARAMS:{}", driverJobDTO);
        // 修改点：远程调用返回 PageResponse 可能为 null，统一通过 Rx 安全取值，避免 NPE
        PageResponse<DriverJobDTO> result = driverJobFeign.findByPage(driverJobDTO);
        List<DriverJobDTO> items = Rx.items(result);
        log.info("历史列表 RESULT:{}", items);

        List<CargoTranTaskDTO> cargoTranTaskDTOS = items.stream().map(item -> CargoTranTaskDTO.builder()
                .taskNo(item.getTaskTransportId() == null ? null : String.valueOf(item.getTaskTransportId()))
                .actualArrivalTime(item.getActualArrivalTime())
                .status(item.getStatus())
                .id(item.getId() == null ? null : String.valueOf(item.getId()))
                .build()).collect(Collectors.toList());
        log.info("历史列表 返回：{}", cargoTranTaskDTOS);
        return Result.ok().put("data", PageResponse.<CargoTranTaskDTO>builder()
                .counts(result != null ? result.getCounts() : 0L).page(page).pagesize(pagesize).pages(result != null ? result.getPages() : 0L)
                .items(cargoTranTaskDTOS).build());

    }

    @SneakyThrows
    @Operation(summary = "在途任务")
    @ResponseBody
    @GetMapping("onTheWay")
    public Result onTheWay() {

        //  获取司机id  并放入参数
        String driverId = RequestContext.getUserId();

        DriverJobDTO driverJobDTO = new DriverJobDTO();
        driverJobDTO.setPage(1);
        driverJobDTO.setPageSize(1);
        driverJobDTO.setStatus(DriverJobStatus.PROCESSING.getCode());
        driverJobDTO.setDriverId(Long.valueOf(driverId));

        log.info("在途任务 PARAMS:{}", driverJobDTO);
        // 修改点：远程调用返回 PageResponse 可能为 null，统一判空避免 NPE
        PageResponse<DriverJobDTO> result = driverJobFeign.findByPage(driverJobDTO);
        // 在途只会有一个
        if (result == null || result.getItems() == null || result.getCounts() <= 0) {
            return Result.ok().put("data", new CargoTranTaskDTO());
        }
        log.info("在途任务 RESULT:{}", result.getItems());
        DriverJobDTO driverJob = result.getItems().get(0);

        Map<Long, TaskTransportDTO> transportTaskDTOMap = new HashMap<>();
        TaskTransportDTO transportTaskDTO = transportTaskFeign.findById(driverJob.getTaskTransportId());
        if (transportTaskDTO == null) {
            log.warn("运输任务不存在: taskTransportId={}", driverJob.getTaskTransportId());
            return Result.error(500, "运输任务不存在");
        }
        transportTaskDTOMap.put(transportTaskDTO.getId(), transportTaskDTO);

        //查询地址
        Set<Long> agencySet = new HashSet<>();
        agencySet.add(driverJob.getStartOrgId());
        agencySet.add(driverJob.getEndOrgId());
        CompletableFuture<List<OrgDTO>> agencyListFuture = PdCompletableFuture.agencyListFuture(orgFeign, null, agencySet, null);
        List<OrgDTO> agencyList = agencyListFuture.get();

        // 查询地区
        Set<Long> areaSet = new HashSet<>();
        areaSet.addAll(agencyList.stream().map(item -> item.getProvinceId()).collect(Collectors.toSet()));
        areaSet.addAll(agencyList.stream().map(item -> item.getCityId()).collect(Collectors.toSet()));
        areaSet.addAll(agencyList.stream().map(item -> item.getCountyId()).collect(Collectors.toSet()));
        CompletableFuture<Map> areaMapFuture = PdCompletableFuture.areaMapFuture(areaFeign, null, areaSet);

        Set<Long> userSet = agencyList.stream().map(item -> item.getManagerId()).collect(Collectors.toSet());
        CompletableFuture<Map> userMapFuture = PdCompletableFuture.userMapFuture(userFeign, userSet, null, null, null);


        Map areaMap = areaMapFuture.get();
        Map userMap = userMapFuture.get();

        Map<String, AgencyVo> agencyMap = agencyList.stream().map(item -> {
            AgencyVo agencyVo = new AgencyVo();
            BeanUtils.copyProperties(item, agencyVo);
            agencyVo.setId(item.getId().toString());
            agencyVo.setProvince((AreaSimpleVo) areaMap.get(item.getProvinceId()));
            agencyVo.setCity((AreaSimpleVo) areaMap.get(item.getCityId()));
            agencyVo.setCounty((AreaSimpleVo) areaMap.get(item.getCountyId()));
            agencyVo.setManager((SysUserVo) userMap.get(item.getManagerId()));
            return agencyVo;
        }).collect(Collectors.toMap(AgencyVo::getId, vo -> vo));

        CargoTranTaskDTO cargoTranTaskDTO = new CargoTranTaskDTO(driverJobDTO, transportTaskDTOMap, agencyMap);
        return Result.ok().put("data", cargoTranTaskDTO);
    }

    @SneakyThrows
    @Operation(summary = "获取车次明细")
    @Parameter(name = "id", description = "主键", required = true)
    @ResponseBody
    @GetMapping("detail")
    public Result detail(String id) {
        DriverJobDTO driverJobDTO = driverJobFeign.findById(Long.valueOf(id));
        if (driverJobDTO == null) {
            return Result.error(404, "司机作业单不存在");
        }
        // 归属校验：作业单必须属于当前登录司机，防止越权查看他人车次明细
        Result deny = OwnershipAssert.checkEquals(String.valueOf(driverJobDTO.getDriverId()), RequestContext.getUserId(), "无权查看他人作业单");
        if (deny != null) {
            return deny;
        }

        Map<Long, TaskTransportDTO> transportTaskDTOMap = new HashMap<>();
        TaskTransportDTO transportTaskDTO = transportTaskFeign.findById(driverJobDTO.getTaskTransportId());
        if (transportTaskDTO == null) {
            return Result.error(404, "运输任务不存在");
        }
        transportTaskDTOMap.put(transportTaskDTO.getId(), transportTaskDTO);

        //查询地址
        Set<Long> agencySet = new HashSet<>();
        agencySet.add(driverJobDTO.getStartOrgId());
        agencySet.add(driverJobDTO.getEndOrgId());
        CompletableFuture<List<OrgDTO>> agencyListFuture = PdCompletableFuture.agencyListFuture(orgFeign, null, agencySet, null);
        List<OrgDTO> agencyList = agencyListFuture.get();

        // 查询地区
        Set<Long> areaSet = new HashSet<>();
        areaSet.addAll(agencyList.stream().map(item -> item.getProvinceId()).collect(Collectors.toSet()));
        areaSet.addAll(agencyList.stream().map(item -> item.getCityId()).collect(Collectors.toSet()));
        areaSet.addAll(agencyList.stream().map(item -> item.getCountyId()).collect(Collectors.toSet()));
        CompletableFuture<Map> areaMapFuture = PdCompletableFuture.areaMapFuture(areaFeign, null, areaSet);

        Set<Long> userSet = agencyList.stream().map(item -> item.getManagerId()).collect(Collectors.toSet());
        CompletableFuture<Map> userMapFuture = PdCompletableFuture.userMapFuture(userFeign, userSet, null, null, null);


        Map areaMap = areaMapFuture.get();
        Map userMap = userMapFuture.get();

        Map<String, AgencyVo> agencyMap = agencyList.stream().map(item -> {
            AgencyVo agencyVo = new AgencyVo();
            BeanUtils.copyProperties(item, agencyVo);
            agencyVo.setId(item.getId().toString());
            agencyVo.setProvince((AreaSimpleVo) areaMap.get(item.getProvinceId()));
            agencyVo.setCity((AreaSimpleVo) areaMap.get(item.getCityId()));
            agencyVo.setCounty((AreaSimpleVo) areaMap.get(item.getCountyId()));
            agencyVo.setManager((SysUserVo) userMap.get(item.getManagerId()));
            return agencyVo;
        }).collect(Collectors.toMap(AgencyVo::getId, vo -> vo));

        CargoTranTaskDTO cargoTranTaskDTO = new CargoTranTaskDTO(driverJobDTO, transportTaskDTOMap, agencyMap);
        return Result.ok().put("data", cargoTranTaskDTO);
    }

    @Operation(summary = "获取货物明细(不分页)")
    @Parameters({
            @Parameter(name = "id", description = "主键", required = true),
            @Parameter(name = "keyword", description = "搜索条件", required = false)
    })
    @ResponseBody
    @GetMapping("orders")
    public Result orders(String keyword, String id) {
        log.info("获取货物明细：{} {}", keyword, id);
        if (StringUtils.isBlank(id)) {
            return Result.ok().put("data", PageResponse.<String>builder()
                    .counts(0L).page(0).pagesize(0).pages(0L).build());
        }

        DriverJobDTO driverJob = driverJobFeign.findById(Long.valueOf(id));
        log.info("获取货物明细 司机任务： {}", driverJob);
        if (driverJob == null) {
            return Result.ok().put("data", PageResponse.<String>builder()
                    .counts(0L).page(0).pagesize(0).pages(0L).build());
        }
        // 归属校验：作业单必须属于当前登录司机，防止越权拉取他人货物明细
        Result deny = OwnershipAssert.checkEquals(String.valueOf(driverJob.getDriverId()), RequestContext.getUserId(), "无权查看他人作业单");
        if (deny != null) {
            return deny;
        }
        TaskTransportDTO transportTaskDTO = transportTaskFeign.findById(driverJob.getTaskTransportId());
        List<String> result = transportTaskDTO.getTransportOrderIds().stream()
                .map(String::valueOf).collect(Collectors.toList());
        log.info("获取货物明细 运输任务： {}", transportTaskDTO);

        if (StringUtils.isNotBlank(keyword)) {
            result = result.stream().filter(item -> item.contains(keyword)).collect(Collectors.toList());
        }

        log.info("获取货物明细 最终返回： {}", result);

        return Result.ok().put("data", PageResponse.<String>builder()
                .counts(Long.valueOf(transportTaskDTO.getTransportOrderCount())).page(1).pagesize(transportTaskDTO.getTransportOrderCount()).pages(1L)
                .items(result).build());
    }

    @Operation(summary = "提货")
    @ResponseBody
    @PutMapping("pickUp")
    public Result pickUp(@RequestBody TaskTransportDTO taskTransportDTO) {

        //  获取司机id  并放入参数
        String driverId = RequestContext.getUserId();

        // 先按请求体id取作业单，归属校验必须在任何业务判断之前
        DriverJobDTO driverJob = driverJobFeign.findById(taskTransportDTO.getId());
        if (driverJob == null) {
            return Result.error(400, "司机作业单不存在");
        }
        Result ownershipDeny = OwnershipAssert.checkEquals(String.valueOf(driverJob.getDriverId()), driverId, "无权操作他人作业单");
        if (ownershipDeny != null) {
            return ownershipDeny;
        }

        Long taskTransportId = driverJob.getTaskTransportId();
        if (taskTransportId == null) {
            return Result.error(400, "运输任务ID为空");
        }
        // 获取运输任务（在途判断与后续更新都要用）
        TaskTransportDTO taskTransport = transportTaskFeign.findById(taskTransportId);
        if (taskTransport == null) {
            return Result.error(400, "运输任务不存在");
        }

        // 校验当前司机是否存在在途作业单
        DriverJobDTO processingQuery = new DriverJobDTO();
        processingQuery.setPage(1);
        processingQuery.setPageSize(10);
        processingQuery.setStatus(DriverJobStatus.PROCESSING.getCode());
        processingQuery.setDriverId(Long.valueOf(driverId));
        PageResponse<DriverJobDTO> processingPage = driverJobFeign.findByPage(processingQuery);
        List<DriverJobDTO> processingJobs = Rx.items(processingPage);
        if (!processingJobs.isEmpty()) {
            // 断点续提放行条件：在途作业单有且只有当前这一单，且其运输任务并未推进到 PROCESSING
            // （说明上次提货在"修改运输任务状态"之前 Feign 失败，作业单残留 PROCESSING，允许重试续提）
            boolean onlyCurrentJob = processingJobs.stream()
                    .allMatch(item -> driverJob.getId().equals(item.getId()));
            boolean transportNotProcessing = !TransportTaskStatus.PROCESSING.getCode().equals(taskTransport.getStatus());
            if (!(onlyCurrentJob && transportNotProcessing)) {
                return Result.error(ErrorCode.ONTHEWAY, "在途任务尚未结束，无法提货");
            }
            log.info("断点续提：作业单{}上次提货中途失败，允许继续提货", driverJob.getId());
        }

        Long startOrgId = driverJob.getStartOrgId();
        // Feign 直接返回裸 DTO，远程不存在或降级时为 null
        OrgDTO org = orgFeign.get(startOrgId);
        if (org == null) {
            return Result.error(ErrorCode.ONTHEWAY, "起始机构不存在");
        }
        // 修改司机作业单
        DriverJobDTO driverJobUpdate = new DriverJobDTO();
        driverJobUpdate.setStatus(DriverJobStatus.PROCESSING.getCode());
        driverJobUpdate.setStartHandover(org.getManager());
        //driverJobUpdate.setActualArrivalTime(LocalDateTime.now());
        DriverJobDTO driverJobResult = driverJobFeign.updateById(driverJob.getId(), driverJobUpdate);
        if (driverJobResult == null) {
            // Feign 走了降级返回 null，远程写未生效，抛异常回滚全局事务，避免留下半提货物状态
            log.error("提货失败：司机作业单更新未生效 driverJobId={}", driverJob.getId());
            throw new PdException("司机作业单更新失败", 500);
        }
        // 修改运输任务表
        TaskTransportDTO taskTransportUpdate = new TaskTransportDTO();
        taskTransportUpdate.setIds(taskTransport.getIds());
        taskTransportUpdate.setTransportOrderIds(taskTransport.getTransportOrderIds());
        taskTransportUpdate.setStatus(TransportTaskStatus.PROCESSING.getCode());
        taskTransportUpdate.setCargoPicture(taskTransportDTO.getCargoPicture());
        taskTransportUpdate.setPickupPicture(taskTransportDTO.getPickupPicture());
        taskTransportUpdate.setPickupLatitude(taskTransportDTO.getPickupLatitude());
        taskTransportUpdate.setPickupLongitude(taskTransportDTO.getPickupLongitude());
        taskTransportUpdate.setActualPickUpTime(LocalDateTime.now());
        taskTransportUpdate.setActualDepartureTime(taskTransportUpdate.getActualPickUpTime());

        TaskTransportDTO transportTaskResult = transportTaskFeign.updateById(taskTransportId, taskTransportUpdate);
        if (transportTaskResult == null) {
            log.error("提货失败：运输任务更新未生效 taskTransportId={}", taskTransportId);
            throw new PdException("运输任务更新失败", 500);
        }


        List<Long> transportOrderIds = taskTransport.getTransportOrderIds();

        // 修改运单
        for (Long transportOrderId : transportOrderIds) {
            TransportOrderDTO transportOrderDTO = new TransportOrderDTO();
            transportOrderDTO.setStatus(TransportOrderStatus.LOADED.getCode());
            TransportOrderDTO updateResult = transportOrderFeign.updateById(transportOrderId, transportOrderDTO);
            if (updateResult == null) {
                log.error("提货失败：运单状态更新未生效 transportOrderId={}", transportOrderId);
                throw new PdException("运单状态更新失败", 500);
            }
            log.info("修改运单状态: {} {}", transportOrderId, transportOrderDTO);
        }

        // 修改订单
        for (Long transportOrderId : transportOrderIds) {
            // 获取订单id
            TransportOrderDTO transportOrder = transportOrderFeign.findById(transportOrderId);
            if (transportOrder == null || transportOrder.getOrderId() == null) {
                log.warn("运单不存在或无关联订单: transportOrderId={}", transportOrderId);
                continue;
            }
            String orderId = String.valueOf(transportOrder.getOrderId());
            // 修改订单状态
            OrderDTO orderDTO = new OrderDTO();
            // 修复：原实现误将枚举值字符串 "IN_TRANSIT" 写入 currentAgencyId，应写入起始机构ID
            orderDTO.setCurrentAgencyId(startOrgId == null ? null : String.valueOf(startOrgId));
            orderDTO.setStatus(OrderStatus.IN_TRANSIT.getCode());
            OrderDTO orderUpdateResult = orderFeign.updateById(orderId, orderDTO);
            if (orderUpdateResult == null) {
                log.error("提货失败：订单状态更新未生效 orderId={}", orderId);
                throw new PdException("订单状态更新失败", 500);
            }
            log.info("修改订单状态和当前机构: {} {}", orderId, orderDTO);
        }
        return Result.ok();
    }

    @Operation(summary = "交付")
    @ResponseBody
    @PutMapping("finish")
    public Result finish(@RequestBody TaskTransportDTO taskTransportDTO) {
        if (taskTransportDTO == null || taskTransportDTO.getId() == null) {
            return Result.error(400, "运输任务ID不能为空");
        }

        DriverJobDTO driverJob = driverJobFeign.findById(taskTransportDTO.getId());
        if (driverJob == null) {
            return Result.error(400, "司机作业单不存在");
        }
        // 归属校验：作业单必须属于当前登录司机，防止越权交付
        Result ownershipDeny = OwnershipAssert.checkEquals(String.valueOf(driverJob.getDriverId()), RequestContext.getUserId(), "无权操作他人作业单");
        if (ownershipDeny != null) {
            return ownershipDeny;
        }
        // 幂等：作业单已完成说明上次交付已走完，App 重试直接返回成功，不重复建派件任务
        if (DriverJobStatus.COMPLETED.getCode().equals(driverJob.getStatus())) {
            log.info("作业单已完成，幂等返回成功 driverJobId={}", driverJob.getId());
            return Result.ok();
        }
        Long taskTransportId = driverJob.getTaskTransportId();
        if (taskTransportId == null) {
            return Result.error(400, "运输任务ID为空");
        }
        Long endOrgId = driverJob.getEndOrgId();
        // Feign 直接返回裸 DTO，远程不存在或降级时为 null
        OrgDTO org = orgFeign.get(endOrgId);
        if (org == null) {
            return Result.error(ErrorCode.ONTHEWAY, "目的机构不存在");
        }
        // 获取全部运单
        TaskTransportDTO taskTransport = transportTaskFeign.findById(taskTransportId);
        if (taskTransport == null) {
            return Result.error(400, "运输任务不存在");
        }
        // 修改司机作业单
        DriverJobDTO driverJobDTO = new DriverJobDTO();
        driverJobDTO.setId(driverJob.getId());
        driverJobDTO.setStatus(DriverJobStatus.COMPLETED.getCode());
        driverJobDTO.setFinishHandover(org.getManager());
        driverJobDTO.setActualArrivalTime(LocalDateTime.now());
        DriverJobDTO driverJobResult = driverJobFeign.updateById(driverJob.getId(), driverJobDTO);
        if (driverJobResult == null) {
            log.error("交付失败：司机作业单更新未生效 driverJobId={}", driverJob.getId());
            throw new PdException("司机作业单更新失败", 500);
        }
        // 修改运输任务表
        TaskTransportDTO taskTransportUpdate = new TaskTransportDTO();
        taskTransportUpdate.setId(taskTransport.getId());
        taskTransportUpdate.setIds(taskTransport.getIds());
        taskTransportUpdate.setTransportOrderIds(taskTransport.getTransportOrderIds());
        taskTransportUpdate.setStatus(TransportTaskStatus.COMPLETED.getCode());
        taskTransportUpdate.setCertificatePicture(taskTransportDTO.getCertificatePicture());
        taskTransportUpdate.setDeliverPicture(taskTransportDTO.getDeliverPicture());
        taskTransportUpdate.setDeliverLatitude(taskTransportDTO.getDeliverLatitude());
        taskTransportUpdate.setDeliverLongitude(taskTransportDTO.getDeliverLongitude());
        taskTransportUpdate.setActualArrivalTime(driverJobDTO.getActualArrivalTime());
        // 修复自赋值 bug：原代码把 actualDeliveryTime 设成它自己（恒为 null），
        // 实际交付时间应使用本次取到的实际到达时间
        taskTransportUpdate.setActualDeliveryTime(driverJobDTO.getActualArrivalTime());

        TaskTransportDTO transportTaskResult = transportTaskFeign.updateById(taskTransportId, taskTransportUpdate);
        if (transportTaskResult == null) {
            log.error("交付失败：运输任务更新未生效 taskTransportId={}", taskTransportId);
            throw new PdException("运输任务更新失败", 500);
        }

        log.info("到达机构：{}.运输任务更新状态：{}", endOrgId, taskTransportUpdate);

        List<Long> transportOrderIds = taskTransport.getTransportOrderIds();

        // 判断送达网点是否是终点  如果是终点 更改订单状态
        List<AgencyScopeDto> agencyScope = agencyScopeFeign.findAllAgencyScope(null, String.valueOf(endOrgId), null, null);
        // 当前网点业务范围
        List<String> areaIds = agencyScope.stream().map(item -> item.getAreaId()).collect(Collectors.toList());
        log.info("当点机构：{} 业务范围：{}", endOrgId, areaIds);
        // 修改订单
        for (Long transportOrderId : transportOrderIds) {
            // 修改运单
            TransportOrderDTO transportOrderDTO = new TransportOrderDTO();

            // 获取订单id
            TransportOrderDTO transportOrder = transportOrderFeign.findById(transportOrderId);
            if (transportOrder == null || transportOrder.getOrderId() == null) {
                log.error("交付失败：运单不存在或未关联订单 transportOrderId={}", transportOrderId);
                throw new PdException("运单不存在或未关联订单", 400);
            }
            String orderId = String.valueOf(transportOrder.getOrderId());

            // 修改订单状态
            OrderDTO orderDTO = orderFeign.findById(orderId);
            if (orderDTO == null) {
                log.error("交付失败：订单不存在 orderId={}", orderId);
                throw new PdException("订单不存在", 400);
            }
            OrderDTO orderDTOUpdate = new OrderDTO();
            orderDTOUpdate.setCurrentAgencyId(String.valueOf(taskTransport.getEndOrgId()));
            //查询订单位置信息
            OrderLocationDto orderLocationDto = orderFeign.selectByOrderId(orderId);
            boolean isFinal = false;
            if(orderLocationDto==null){
                if(areaIds.contains(orderDTO.getReceiverCountyId())){
                    isFinal=true;
                }
            }else{
                if(StringUtils.equals(String.valueOf(endOrgId),orderLocationDto.getReceiveAgentId())){
                    isFinal=true;
                }
            }
//            if (areaIds.contains(orderDTO.getReceiverCountyId())) {
            if(isFinal){
                log.info("订单到达最终网点：{},{}", transportOrderId, orderId);
                // 到达目的地
                transportOrderDTO.setStatus(TransportOrderStatus.ARRIVED_END.getCode());
                orderDTOUpdate.setStatus(OrderStatus.OUTLETS_EX_WAREHOUSE.getCode());
                // 幂等关键：先按 orderId + DISPATCH 类型查派件任务，
                // 已存在非取消任务则直接复用，防止 App 重试新建第二条导致下游 getOne 抛 TooManyResultsException
                TaskPickupDispatchDTO dispatchQuery = new TaskPickupDispatchDTO();
                dispatchQuery.setOrderId(Long.valueOf(orderDTO.getId()));
                dispatchQuery.setTaskType(PickupDispatchTaskType.DISPATCH.getCode());
                List<TaskPickupDispatchDTO> existDispatchTasks = pickupDispatchTaskFeign.findAll(dispatchQuery);
                TaskPickupDispatchDTO existDispatchTask = null;
                if (existDispatchTasks != null) {
                    for (TaskPickupDispatchDTO item : existDispatchTasks) {
                        if (!PickupDispatchTaskStatus.CANCELLED.getCode().equals(item.getStatus())) {
                            existDispatchTask = item;
                            break;
                        }
                    }
                }

                if (existDispatchTask != null) {
                    log.info("派件任务已存在，幂等复用 orderId={}, taskId={}", orderId, existDispatchTask.getId());
                } else {
                    // 仅首次创建时才需要分配快递员
                    String courierId = getCourierId(orderDTO);
                    if (StringUtils.isBlank(courierId)) {
                        //岗位id
                        Long stationId = StaticStation.COURIER_ID;
                        // Feign 直接返回裸 List，降级时为 null，经 Rx.list 安全取值
                        List<UserDTO> userList = Rx.list(userFeign.list(null, stationId, null, endOrgId));
                        if (!userList.isEmpty()) {
                            UserDTO user = userList.get(0);
                            courierId = user.getId().toString();
                        }
                    }

                    log.info("网点出库分配快递员:{},快递员:{}", endOrgId, courierId);

                    TaskPickupDispatchDTO pickupDispatchTaskDTO = new TaskPickupDispatchDTO();
                    pickupDispatchTaskDTO.setOrderId(Long.valueOf(orderDTO.getId()));
                    pickupDispatchTaskDTO.setTaskType(PickupDispatchTaskType.DISPATCH.getCode());
                    pickupDispatchTaskDTO.setStatus(PickupDispatchTaskStatus.PENDING.getCode());
                    pickupDispatchTaskDTO.setAssignedStatus(StringUtils.isNotBlank(courierId) ? PickupDispatchTaskAssignedStatus.DISTRIBUTED.getCode() : PickupDispatchTaskAssignedStatus.MANUAL_DISTRIBUTED.getCode());
                    pickupDispatchTaskDTO.setCreateTime(LocalDateTime.now());
                    pickupDispatchTaskDTO.setOrgId(endOrgId);
                    pickupDispatchTaskDTO.setCourierId(Long.valueOf(courierId));
                    pickupDispatchTaskDTO.setEstimatedStartTime(LocalDateTime.now());
                    pickupDispatchTaskDTO.setEstimatedEndTime(LocalDateTime.now().plusHours(1));
                    TaskPickupDispatchDTO savedDispatchTask = pickupDispatchTaskFeign.save(pickupDispatchTaskDTO);
                    if (savedDispatchTask == null) {
                        log.error("交付失败：派件任务保存未生效 orderId={}", orderId);
                        throw new PdException("派件任务保存失败", 500);
                    }
                    log.info("保存快递员派件任务信息：{}", pickupDispatchTaskDTO);
                }
            } else {
                transportOrderDTO.setStatus(TransportOrderStatus.ARRIVED.getCode());
                orderDTOUpdate.setStatus(OrderStatus.IN_TRANSIT.getCode());
            }
            TransportOrderDTO transportOrderUpdateResult = transportOrderFeign.updateById(transportOrderId, transportOrderDTO);
            if (transportOrderUpdateResult == null) {
                log.error("交付失败：运单状态更新未生效 transportOrderId={}", transportOrderId);
                throw new PdException("运单状态更新失败", 500);
            }
            log.info("修改运单状态: {} {}", transportOrderId, transportOrderDTO);
            OrderDTO orderUpdateResult = orderFeign.updateById(orderId, orderDTOUpdate);
            if (orderUpdateResult == null) {
                log.error("交付失败：订单状态更新未生效 orderId={}", orderId);
                throw new PdException("订单状态更新失败", 500);
            }
            log.info("修改订单状态和当前机构: {} {}", orderId, orderDTOUpdate);
        }

        return Result.ok();
    }

    private String getCourierId(OrderDTO orderDTO) {
        // 修复：派件应按【收件区县】查快递员范围，原误用发件区县（深圳）导致查不到广州快递员
        String receiverCountyId = orderDTO.getReceiverCountyId();
        List<CourierScopeDto> courierScopeDtoList = courierScopeFeign.findAllCourierScope(receiverCountyId, null);
        if(courierScopeDtoList==null || courierScopeDtoList.size()==0){
            return "";
        }
        String location = EntCoordSyncJob.getCoordinate(orderDTO.getReceiverAddress());
        Result res = calcuateCourier(location, courierScopeDtoList);
        if (!res.get("code").toString().equals("0")) {
            // 百度地图不可用导致点选失败：确定性取收件区县配置的第一个快递员，
            // 不依赖外网、结果可重复，保证末端派件一定有人可派（广州天河对应 9202）
            String fallbackUserId = courierScopeDtoList.get(0).getUserId();
            log.info("百度点选失败，按收件区县[{}]确定性匹配快递员[{}]", receiverCountyId, fallbackUserId);
            return fallbackUserId;
        }
        return res.get("userId").toString();
    }

    private Result calcuateCourier(String location, List<CourierScopeDto> courierScopeDtoList) {
        try{
            Map courierMap = Maps.newHashMap();
            for (CourierScopeDto courierScopeDto : courierScopeDtoList) {
                List<List<Map>> mutiPoints = courierScopeDto.getMutiPoints();
                for (List<Map> list : mutiPoints) {
                    for (Map map :list) {
                        String point = getPoint(map);
                        Double distance = EntCoordSyncJob.getDistance(location,point);
                        courierMap.put(courierScopeDto.getUserId(),distance);
                    }
                }
            }
            //获取map中最小距离的网点
            List<Map.Entry<String, Double>> list = new ArrayList(courierMap.entrySet());
            list.sort(Comparator.comparingDouble(Map.Entry::getValue));
            String userId = list.get(0).getKey();
            return Result.ok().put("userId",userId);
        }catch (Exception e){
            log.error("操作失败", e);
            return Result.error(5000,"获取最短距离快递员失败");
        }
    }
    private String getPoint(Map pointMap) {
        String lng = pointMap.getOrDefault("lng", "").toString();
        String lat = pointMap.getOrDefault("lat", "").toString();
        return lng + "," + lat;
    }



    /**
     * 在途异常上报（D-57 · 定案 T-4 · 2026-10-08 新增）
     *
     * <p>司机在运输途中上报车辆故障/货物损失/延误，<b>落库复用 pd-netty 的
     * {@code pd_alarm_record}</b>——与 GPS 自动告警同表，按 alarmType 区分来源。</p>
     *
     * <p><b>入参遵循 D-46「类型 + 照片 + 备注」三要素</b>，由 pd-netty 侧统一校验；
     * 无照片凭证的异常不予受理。</p>
     *
     * <p><b>上报人ID 由服务端从 token 取</b>（{@link RequestContext#getUserId()}），
     * 不接受前端传他人 ID——否则可冒名上报。</p>
     *
     * @param dto 在途异常上报入参
     * @return 落库后的告警记录（含 id 供前端追单）
     */
    @Operation(summary = "在途异常上报（车辆故障/货物损失/延误）")
    @PostMapping("exception/report")
    public Result reportException(@RequestBody DriverExceptionReportDTO dto) {
        String driverId = RequestContext.getUserId();
        if (dto == null) {
            return Result.error(400, "上报内容不能为空");
        }
        // 服务端覆盖上报人：绝不相信前端传入的 reporterId（防冒名上报）
        dto.setReporterId(driverId);
        dto.setCourier(false);
        Result result = driverExceptionReportFeign.report(dto);
        log.info("司机在途异常上报: driverId={}, type={}, task={}", driverId, dto.getExceptionType(), dto.getTransportTaskId());
        return result;
    }
} 
