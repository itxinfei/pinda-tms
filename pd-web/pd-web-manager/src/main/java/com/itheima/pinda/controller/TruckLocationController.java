package com.itheima.pinda.controller;

import com.itheima.pinda.DTO.TaskTransportDTO;
import com.itheima.pinda.DTO.UserDTO;
import com.itheima.pinda.DTO.transportline.TransportTripsTruckDriverDto;
import com.itheima.pinda.DTO.truck.TruckDto;
import com.itheima.pinda.DTO.truck.TruckTypeDto;
import com.itheima.pinda.enums.transporttask.TransportTaskStatus;
import com.itheima.pinda.feign.TransportTaskFeign;
import com.itheima.pinda.feign.UserFeign;
import com.itheima.pinda.feign.transportline.TransportTripsFeign;
import com.itheima.pinda.feign.truck.TruckFeign;
import com.itheima.pinda.feign.truck.TruckTypeFeign;
import com.itheima.pinda.vo.base.transforCenter.business.TruckLocationVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("transfor-center")
@Tag(name = "位置查询")
@Slf4j
public class TruckLocationController {
    @Autowired
    private UserFeign userFeign;
    @Autowired
    private TruckFeign truckFeign;
    @Autowired
    private TruckTypeFeign truckTypeFeign;
    @Autowired
    private TransportTaskFeign transportTaskFeign;
    @Autowired
    private TransportTripsFeign transportTripsFeign;


    @Operation(summary = "获取车辆位置详情")
    @GetMapping("truck-place-info/{id}")
    public TruckLocationVo findTruckById(@PathVariable(name = "id") String id) {
        TruckLocationVo truckLocationVo = new TruckLocationVo();
        TruckDto truck = truckFeign.fineById(id);
        if (ObjectUtils.isEmpty(truck)) {
            return truckLocationVo;
        }
        truckLocationVo.setLicensePlate(truck.getLicensePlate());

        TruckTypeDto truckTypeDto = truckTypeFeign.fineById(truck.getTruckTypeId());
        if (!ObjectUtils.isEmpty(truckTypeDto)) {
            truckLocationVo.setTruckTypeName(truckTypeDto.getName());
        }

        TaskTransportDTO taskTransportDto = new TaskTransportDTO();
        taskTransportDto.setTruckId(Long.valueOf(truck.getId()));
        taskTransportDto.setStatus(TransportTaskStatus.PROCESSING.getCode());
        List<TaskTransportDTO> transportTaskDtos = transportTaskFeign.findAll(taskTransportDto);
        if (CollectionUtils.isEmpty(transportTaskDtos)) {
            return truckLocationVo;
        }

        taskTransportDto = transportTaskDtos.get(0);
        Long currentTripsId = taskTransportDto.getTripsId();
        List<TransportTripsTruckDriverDto> transportTripsTruckDriverDtos = transportTripsFeign.findAllTruckDriverTransportTrips(
                currentTripsId == null ? null : String.valueOf(currentTripsId), truck.getId(), null);
        if (CollectionUtils.isEmpty(transportTripsTruckDriverDtos)) {
            return truckLocationVo;
        }

        TransportTripsTruckDriverDto transportTripsTruckDriverDto = transportTripsTruckDriverDtos.get(0);
        String userId = transportTripsTruckDriverDto.getUserId();
        // 远程调用可能返回 null，直接判空，避免 NPE
        UserDTO user = userFeign.get(Long.valueOf(userId));
        if (user != null) {
            truckLocationVo.setName(user.getName());
            truckLocationVo.setMobile(user.getMobile());
            truckLocationVo.setAvatar(user.getAvatar());
        }

        return truckLocationVo;
    }
}
