package com.itheima.pinda.service.impl;

import com.itheima.pinda.DTO.*;
import com.itheima.pinda.DTO.transportline.TransportLineDto;
import com.itheima.pinda.DTO.transportline.TransportTripsDto;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
            List<String> transportOrderIds = new ArrayList<>();
            orderClassifys.forEach(orderClassify -> {
                if (orderClassify.isNew()) {
                    // 新订单  更新运单信息
                    orderClassify.getOrders().forEach(item -> {
                        // 创建运单（如果不存在则创建，已存在则直接返回）
                        TransportOrderDTO transportOrderDto = transportOrderFeign.findByOrderId(item.getId());
                        if (transportOrderDto == null) {
                            transportOrderDto = new TransportOrderDTO();
                            transportOrderDto.setOrderId(item.getId());
                            // 使用运单状态枚举，确保状态一致
                            transportOrderDto.setStatus(TransportOrderStatus.CREATED.getCode());
                            transportOrderDto.setSchedulingStatus(TransportOrderSchedulingStatus.TO_BE_SCHEDULED.getCode());
                            transportOrderDto = transportOrderFeign.save(transportOrderDto);
                            log.info("创建运单: {}", transportOrderDto.getId());
                        }
                        transportOrderIds.add(transportOrderDto.getId());

                        // 更新订单状态为待装车
                        OrderDTO orderDto = new OrderDTO();
                        orderDto.setStatus(OrderStatus.FOR_LOADING.getCode());
                        orderFeign.updateById(item.getId(), orderDto);
                        log.info("更新订单状态为待装车: {}", item.getId());
                    });
                }
                //  查询运单信息构建运单集合
                List<String> orderIds = orderClassify.getOrders().stream()
                        .map(OrderDTO::getId)
                        .collect(Collectors.toList());
                List<TransportOrderDTO> transportOrders = transportOrderFeign.findByOrderIds(orderIds);
                transportOrderIds.addAll(transportOrders.stream()
                        .map(TransportOrderDTO::getId)
                        .collect(Collectors.toList()));
            });

            // 创建运输任务
            TaskTransportDTO taskTranSportDto = new TaskTransportDTO();
            taskTranSportDto.setStartAgencyId(cacheLineDetail.getStartAgencyId());
            taskTranSportDto.setEndAgencyId(cacheLineDetail.getEndAgencyId());
            taskTranSportDto.setStatus(TransportTaskStatus.PENDING.getCode());
            taskTranSportDto.setAssignedStatus(TransportTaskAssignedStatus.TO_BE_DISTRIBUTED.getCode());
            taskTranSportDto.setLoadingStatus(TransportTaskLoadingStatus.HALF.getCode());
            taskTranSportDto.setTransportOrderIds(transportOrderIds);
            taskTranSportDto = transportTaskFeign.save(taskTranSportDto);

            log.info("创建运输任务: {}", taskTranSportDto);

            // 线路管理运输任务 并返回
            result.put(cacheLineDetail.getTransportLineId(), taskTranSportDto);

        });
        return result;
    }

    @Override
    public void updateTransportTask(List<OrderLineTripsTruckDriverDTO> orderLineTripsTruckDriverDTOS, Map<String, TaskTransportDTO> transportTaskMap) {
        log.info("updateTransportTask:begin");
        orderLineTripsTruckDriverDTOS.forEach(orderLineTripsTruckDriverDTO -> {
            String transportLineId = orderLineTripsTruckDriverDTO.getTransportLineId();
            TripsTruckDriverDTO tripsTruckDriver = orderLineTripsTruckDriverDTO.getTripsTruckDriverDTO();

            TransportTripsDto transportTrips = transportTripsFeign.fineById(tripsTruckDriver.getTripsId());
            log.info("查询线路信息：{}", transportTrips);
            String[] departures = transportTrips.getDepartureTime().split(":");
            LocalDateTime departureDate = LocalDateTime.now().withHour(Integer.parseInt(departures[0])).withMinute(Integer.parseInt(departures[1])).withSecond(00);

            TransportLineDto transportLine = transportLineFeign.fineById(transportLineId);
            LocalDateTime arrivalTime = departureDate.plusMinutes(transportLine.getEstimatedTime().longValue());

            TaskTransportDTO taskTransportDTO = transportTaskMap.get(transportLineId);

            // 创建司机任务
            DriverJobDTO driverJobDto = new DriverJobDTO();
            driverJobDto.setStartAgencyId(taskTransportDTO.getStartAgencyId());
            driverJobDto.setEndAgencyId(taskTransportDTO.getEndAgencyId());
            driverJobDto.setStatus(DriverJobStatus.PENDING.getCode());
            driverJobDto.setDriverId(tripsTruckDriver.getDriverId());
            driverJobDto.setTaskTransportId(taskTransportDTO.getId());
            driverJobDto.setPlanDepartureTime(DateUtils.getUTCTime(departureDate));
            driverJobDto.setPlanArrivalTime(DateUtils.getUTCTime(arrivalTime));
            driverJobFeign.save(driverJobDto);
            log.info("创建司机任务:{}", driverJobDto);
            // 更新运输任务信息
            TaskTransportDTO taskTransportDTOUpdate = new TaskTransportDTO();
            taskTransportDTOUpdate.setTransportOrderIds(taskTransportDTO.getTransportOrderIds());
            taskTransportDTOUpdate.setAssignedStatus(TransportTaskAssignedStatus.DISTRIBUTED.getCode());
            taskTransportDTOUpdate.setTransportTripsId(tripsTruckDriver.getTripsId());
            taskTransportDTOUpdate.setTruckId(tripsTruckDriver.getTruckId());
            taskTransportDTOUpdate.setPlanDepartureTime(departureDate);// 计划发车时间
            taskTransportDTOUpdate.setPlanArrivalTime(arrivalTime);// 计划到达时间
            taskTransportDTOUpdate.setPlanPickUpGoodsTime(departureDate);// 计划提货
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
