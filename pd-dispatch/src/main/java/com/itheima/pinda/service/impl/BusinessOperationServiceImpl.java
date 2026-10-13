package com.itheima.pinda.service.impl;

import com.itheima.pinda.DTO.*;
import com.itheima.pinda.DTO.transportline.TransportLineDto;
import com.itheima.pinda.DTO.transportline.TransportTripsDto;
import com.itheima.pinda.common.exception.PdException;
import com.itheima.pinda.common.utils.DateUtils;
import com.itheima.pinda.entity.CacheLineDetailEntity;
import com.itheima.pinda.enums.OrderStatus;
import com.itheima.pinda.enums.driverjob.DriverJobStatus;
import com.itheima.pinda.enums.transportorder.TransportOrderSchedulingStatus;
import com.itheima.pinda.enums.transportorder.TransportOrderStatus;
import com.itheima.pinda.enums.transporttask.TransportTaskAssignedStatus;
import com.itheima.pinda.enums.transporttask.TransportTaskLoadingStatus;
import com.itheima.pinda.enums.transporttask.TransportTaskStatus;
import com.itheima.pinda.feign.DriverJobFeign;
import com.itheima.pinda.feign.OrderFeign;
import com.itheima.pinda.feign.TransportOrderFeign;
import com.itheima.pinda.feign.TransportTaskFeign;
import com.itheima.pinda.feign.transportline.TransportLineFeign;
import com.itheima.pinda.feign.transportline.TransportTripsFeign;
import com.itheima.pinda.service.IBusinessOperationService;
import com.itheima.pinda.utils.IdConvert;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BusinessOperationServiceImpl implements IBusinessOperationService {

    @Autowired
    private TransportTaskFeign transportTaskFeign;

    @Autowired
    private TransportOrderFeign transportOrderFeign;

    @Autowired
    private OrderFeign orderFeign;

    @Autowired
    private TransportTripsFeign transportTripsFeign;

    @Autowired
    private TransportLineFeign transportLineFeign;

    @Autowired
    private DriverJobFeign driverJobFeign;

    @Override
    public Map<String, TaskTransportDTO> createTransportOrderTask(List<OrderLineSimpleDTO> orderLineSimpleDTOS) {
        log.info("创建运输任务，更新订单状态");
        Map<String, TaskTransportDTO> result = new HashMap<>();

        orderLineSimpleDTOS.forEach((orderLineSimpleDTO) -> {
            CacheLineDetailEntity cacheLineDetail = orderLineSimpleDTO.getCacheLineDetailEntity();
            List<OrderClassifyGroupDTO> orderClassifys = orderLineSimpleDTO.getOrderClassifyGroupDTOS();
            log.info("当前线路：{} ,分组订单：{}", cacheLineDetail, orderClassifys);
            // 用 Set 去重：新订单分类在下面 isNew() 分支内经 findByOrderId 收集一次，
            // 循环外又经 findByOrderIds 批量收集一次，同一运单会出现两次；不去重则
            // pd-work 批量插入 work_transport_order_task 产生两行相同关联，撞
            // uk_order_task(tenant_id, transport_order_id, task_id) 唯一键，整批事务回滚。
            Set<Long> transportOrderIds = new LinkedHashSet<>();
            // 本线待推进状态的新订单（pd-oms String 边界）。
            // 关键顺序：先收集、先建运输任务，任务创建成功后再把订单 网点入库(4)→待装车(5)，
            // 否则建任务失败时订单已停在 5 且无任务关联——状态机没有 5→4 回退边，
            // 下轮分类只查状态 4，该单永久漏调度（无补偿）。建任务在前，失败则什么都没改，
            // XXL 下轮重试可干净重来。
            List<String> newOrderIds = new ArrayList<>();
            orderClassifys.forEach(orderClassify -> {
                if (orderClassify.isNew()) {
                    // 新订单 收集运单信息
                    for (String orderId : orderClassify.getOrders()) {
                        // 【P0优化】查询运单（下单时已预生成，理论上应该存在）
                        // 订单ID为 pd-oms String 边界，调 pd-work Feign 前转 Long
                        TransportOrderDTO transportOrderDto = transportOrderFeign.findByOrderId(IdConvert.toLong(orderId));
                        if (transportOrderDto == null) {
                            // 【健壮性】单个订单缺失运单不应中断整轮调度，
                            // 记录错误并跳过该订单，避免影响其它线路/订单的正常调度。
                            log.error("订单[{}]未找到关联运单，跳过该订单调度，请检查订单确认逻辑！", orderId);
                            continue;
                        }
                        transportOrderIds.add(transportOrderDto.getId());
                        newOrderIds.add(orderId);
                    }
                }
                //  查询运单信息构建运单集合（work Feign 已 Long 化，订单 ID 在此边界转换）
                List<Long> orderIds = orderClassify.getOrders().stream()
                        .map(IdConvert::toLong)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                List<TransportOrderDTO> transportOrders = transportOrderFeign.findByOrderIds(orderIds);
                transportOrderIds.addAll(transportOrders.stream()
                        .map(TransportOrderDTO::getId)
                        .collect(Collectors.toList()));
            });

            // 创建运输任务
            TaskTransportDTO taskTranSportDto = new TaskTransportDTO();
            taskTranSportDto.setStartOrgId(cacheLineDetail.getStartOrgId());
            taskTranSportDto.setEndOrgId(cacheLineDetail.getEndOrgId());
            taskTranSportDto.setStatus(TransportTaskStatus.PENDING.getCode());
            taskTranSportDto.setAssignedStatus(TransportTaskAssignedStatus.TO_BE_DISTRIBUTED.getCode());
            taskTranSportDto.setLoadingStatus(TransportTaskLoadingStatus.HALF.getCode());
            taskTranSportDto.setTransportOrderIds(new ArrayList<>(transportOrderIds));
            taskTranSportDto = transportTaskFeign.save(taskTranSportDto);
            if (taskTranSportDto == null || taskTranSportDto.getId() == null) {
                log.error("运输任务创建失败: line={}, start={}, end={}",
                        cacheLineDetail.getTransportLineId(),
                        cacheLineDetail.getStartOrgId(), cacheLineDetail.getEndOrgId());
                throw new PdException("运输任务创建失败，请稍后重试");
            }

            log.info("创建运输任务: {}", taskTranSportDto);

            // 任务创建成功后，才把本线新订单 网点入库(4)→待装车(5)，使其退出下轮分类查询。
            // 若推进失败（含 Feign fallback 吞错返回 null），必须把本线运单的调度标记
            // 复位为待调度，否则会出现"任务已建但订单仍在状态4、运单却已标已调度"的
            // 不一致：下轮重试重新分类这些订单却因运单 schedulingStatus=3 而漏处理。
            try {
                advanceNewOrders(newOrderIds);
                // 调度成功：回写关联运单为"已调度"（此前主链路不回写，运单永远停在待调度）
                markTransportOrdersScheduled(new ArrayList<>(transportOrderIds));
            } catch (Exception e) {
                log.error("运输任务[{}]已创建，但订单状态推进/运单回写失败，开始补偿复位本线运单调度标记",
                        taskTranSportDto.getId(), e);
                resetTransportOrdersForRetry(new ArrayList<>(transportOrderIds));
                throw new PdException("运输任务已创建但订单状态更新失败，已复位待下轮重试，请关注");
            }

            // 线路管理运输任务 并返回（transportLineId 为 pd-base String 边界，作为 Map key 保持 String）
            result.put(IdConvert.toStr(cacheLineDetail.getTransportLineId()), taskTranSportDto);

        });
        return result;
    }

    /**
     * 运输任务创建成功后，批量把新订单由 网点入库(4) 推进到 待装车(5)。
     * 状态机仅允许 4→5；Feign fallback 会吞掉远程异常返回 null，这里显式判空转错。
     */
    private void advanceNewOrders(List<String> newOrderIds) {
        if (newOrderIds == null || newOrderIds.isEmpty()) {
            return;
        }
        for (String orderId : newOrderIds) {
            OrderDTO orderDto = new OrderDTO();
            orderDto.setStatus(OrderStatus.FOR_LOADING.getCode());
            OrderDTO updated = orderFeign.updateById(orderId, orderDto);
            if (updated == null) {
                // Feign fallback：远程调用 pd-oms 失败（断网/熔断/实例不可用）
                throw new PdException("订单状态推进失败（pd-oms 不可用）: orderId=" + orderId);
            }
            log.info("更新订单状态为待装车: {}", orderId);
        }
    }

    /**
     * 调度成功后批量回写运单 schedulingStatus=已调度。
     */
    private void markTransportOrdersScheduled(List<Long> transportOrderIds) {
        if (transportOrderIds == null || transportOrderIds.isEmpty()) {
            return;
        }
        for (Long transportOrderId : transportOrderIds) {
            TransportOrderDTO update = new TransportOrderDTO();
            update.setId(transportOrderId);
            update.setSchedulingStatus(TransportOrderSchedulingStatus.SCHEDULED.getCode());
            TransportOrderDTO updated = transportOrderFeign.updateById(transportOrderId, update);
            if (updated == null) {
                // Feign fallback 吞错返回 null：显式转错，进入上方补偿复位，
                // 避免运单 schedulingStatus 静默停留在待调度、任务却已建的不一致
                throw new PdException("运单调度状态回写失败（pd-work 不可用）: transportOrderId=" + transportOrderId);
            }
        }
    }

    /**
     * 补偿：任务已建但后续状态回写失败时，把本线运单 schedulingStatus 复位为待调度(1)，
     * 使下轮调度能重新分类并关联这些运单。best-effort：复位失败仅记录日志，不掩盖原始异常。
     * 已建运输任务/关联行保留（PENDING 待分配状态，不影响重选重建），如需清理另立人工/兜底任务。
     */
    private void resetTransportOrdersForRetry(List<Long> transportOrderIds) {
        for (Long transportOrderId : transportOrderIds) {
            try {
                TransportOrderDTO reset = new TransportOrderDTO();
                reset.setId(transportOrderId);
                reset.setSchedulingStatus(TransportOrderSchedulingStatus.TO_BE_SCHEDULED.getCode());
                TransportOrderDTO reseted = transportOrderFeign.updateById(transportOrderId, reset);
                if (reseted == null) {
                    log.error("补偿复位运单[{}]失败（pd-work 不可用，fallback 返回 null），需人工核查", transportOrderId);
                } else {
                    log.warn("补偿复位运单[{}] schedulingStatus=待调度", transportOrderId);
                }
            } catch (Exception ex) {
                log.error("补偿复位运单[{}]失败，需人工核查（任务可能已建但运单仍标已调度）", transportOrderId, ex);
            }
        }
    }

    @Override
    public void updateTransportTask(List<OrderLineTripsTruckDriverDTO> orderLineTripsTruckDriverDTOS, Map<String, TaskTransportDTO> transportTaskMap) {
        log.info("updateTransportTask:begin");
        orderLineTripsTruckDriverDTOS.forEach(orderLineTripsTruckDriverDTO -> {
            String transportLineId = orderLineTripsTruckDriverDTO.getTransportLineId();
            TripsTruckDriverDTO tripsTruckDriver = orderLineTripsTruckDriverDTO.getTripsTruckDriverDTO();

            TransportTripsDto transportTrips = transportTripsFeign.fineById(tripsTruckDriver.getTripsId());
            log.info("查询车次信息：{}", transportTrips);
            if (transportTrips == null || StringUtils.isBlank(transportTrips.getDepartureTime())) {
                log.error("车次[{}]不存在或无发车时间，跳过该线路司机作业单生成", tripsTruckDriver.getTripsId());
                transportTaskMap.remove(transportLineId);
                return;
            }
            String[] departures = transportTrips.getDepartureTime().split(":");
            LocalDateTime departureDate = LocalDateTime.now().withHour(Integer.parseInt(departures[0])).withMinute(Integer.parseInt(departures[1])).withSecond(00);

            TransportLineDto transportLine = transportLineFeign.fineById(transportLineId);
            if (transportLine == null || transportLine.getEstimatedTime() == null) {
                log.error("线路[{}]不存在或无预计耗时，跳过该线路司机作业单生成", transportLineId);
                transportTaskMap.remove(transportLineId);
                return;
            }
            LocalDateTime arrivalTime = departureDate.plusMinutes(transportLine.getEstimatedTime().longValue());

            TaskTransportDTO taskTransportDTO = transportTaskMap.get(transportLineId);
            if (taskTransportDTO == null) {
                log.error("线路[{}]对应的运输任务不存在，跳过该线路司机作业单生成", transportLineId);
                return;
            }

            // 创建司机任务（driverId 内部即 Long，直接装配）
            DriverJobDTO driverJobDto = new DriverJobDTO();
            driverJobDto.setStartOrgId(taskTransportDTO.getStartOrgId());
            driverJobDto.setEndOrgId(taskTransportDTO.getEndOrgId());
            driverJobDto.setStatus(DriverJobStatus.PENDING.getCode());
            driverJobDto.setDriverId(tripsTruckDriver.getDriverId());
            driverJobDto.setTaskTransportId(taskTransportDTO.getId());
            driverJobDto.setPlanDepartureTime(DateUtils.shanghaiToUTC(departureDate));
            driverJobDto.setPlanArrivalTime(DateUtils.shanghaiToUTC(arrivalTime));
            DriverJobDTO savedDriverJob = driverJobFeign.save(driverJobDto);
            if (savedDriverJob == null || savedDriverJob.getId() == null) {
                log.error("司机作业单创建失败: taskId={}, driverId={}",
                        taskTransportDTO.getId(), driverJobDto.getDriverId());
                throw new PdException("司机作业单创建失败，请稍后重试");
            }
            log.info("创建司机任务:{}", savedDriverJob);
            // 更新运输任务信息（车次/车辆ID为 pd-base String 边界，落 pd-work BIGINT 列前转换）
            TaskTransportDTO taskTransportDTOUpdate = new TaskTransportDTO();
            taskTransportDTOUpdate.setTransportOrderIds(taskTransportDTO.getTransportOrderIds());
            taskTransportDTOUpdate.setAssignedStatus(TransportTaskAssignedStatus.DISTRIBUTED.getCode());
            taskTransportDTOUpdate.setTripsId(IdConvert.toLong(tripsTruckDriver.getTripsId()));
            taskTransportDTOUpdate.setTruckId(IdConvert.toLong(tripsTruckDriver.getTruckId()));
            taskTransportDTOUpdate.setPlanDepartureTime(departureDate);// 计划发车时间
            taskTransportDTOUpdate.setPlanArrivalTime(arrivalTime);// 计划到达时间
            taskTransportDTOUpdate.setPlanPickUpTime(departureDate);// 计划提货
            taskTransportDTOUpdate.setPlanDeliveryTime(arrivalTime);// 计划交付
            transportTaskFeign.updateById(taskTransportDTO.getId(), taskTransportDTOUpdate);
            log.info("更新运输任务信息:{}", taskTransportDTOUpdate);

            // 临时数据中 删除已经分配的任务
            transportTaskMap.remove(transportLineId);
        });

        // 未分配车辆 司机的运输任务改为手动分配
        transportTaskMap.forEach((transportLineId, taskTransportDTO) -> {
            TaskTransportDTO taskTransportDTOUpdate = new TaskTransportDTO();
            taskTransportDTOUpdate.setTransportOrderIds(taskTransportDTO.getTransportOrderIds());
            taskTransportDTOUpdate.setAssignedStatus(TransportTaskAssignedStatus.MANUAL_DISTRIBUTED.getCode());
            transportTaskFeign.updateById(taskTransportDTO.getId(), taskTransportDTOUpdate);
            log.info("运输任务改为手动分配:{}", taskTransportDTO.getId());
        });
    }

}
