package com.itheima.pinda.controller.transportline;

import java.util.List;

import com.itheima.pinda.common.utils.Result;
import com.itheima.pinda.controller.support.IdConverter;
import com.itheima.pinda.DTO.transportline.TransportTripsTruckDriverDto;
import com.itheima.pinda.entity.transportline.PdTransportTrips;
import com.itheima.pinda.entity.transportline.PdTransportTripsTruckDriver;
import com.itheima.pinda.service.transportline.IPdTransportTripsService;
import com.itheima.pinda.DTO.transportline.TransportTripsDto;

import com.itheima.pinda.service.transportline.IPdTransportTripsTruckDriverService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * TransportTripsController
 */
@RestController
@RequestMapping("base/transportLine/trips")
public class TransportTripsController {
    @Autowired
    private IPdTransportTripsService transportTripsService;
    @Autowired
    private IPdTransportTripsTruckDriverService transportTripsTruckDriverService;

    /**
     * 添加车次
     *
     * @param dto 车次信息
     * @return 车次信息
     */
    @PostMapping("")
    public TransportTripsDto save(@RequestBody TransportTripsDto dto) {
        PdTransportTrips pdTransportTrips = new PdTransportTrips();
        BeanUtils.copyProperties(dto, pdTransportTrips);
        pdTransportTrips.setTransportLineId(IdConverter.toLong(dto.getTransportLineId()));
        pdTransportTrips = transportTripsService.saveTransportTrips(pdTransportTrips);
        return toDto(pdTransportTrips);
    }

    /**
     * 根据id获取车次详情
     *
     * @param id 车次id
     * @return 车次信息
     */
    @GetMapping("/{id}")
    public TransportTripsDto findById(@PathVariable(name = "id") String id) {
        PdTransportTrips pdTransportTrips = transportTripsService.getById(IdConverter.toLong(id));
        if (pdTransportTrips != null) {
            return toDto(pdTransportTrips);
        }
        TransportTripsDto dto = new TransportTripsDto();
        dto.setId(id);
        return dto;
    }

    /**
     * 获取车次列表
     *
     * @param transportLineId 线路id
     * @param ids             车次id列表
     * @return 车次列表
     */
    @GetMapping("")
    public List<TransportTripsDto> findAll(@RequestParam(name = "transportLineId", required = false) String transportLineId,
                                           @RequestParam(name = "ids", required = false) List<String> ids) {
        return transportTripsService.findAll(IdConverter.toLong(transportLineId), IdConverter.toLongList(ids)).stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * 更新车次信息
     *
     * @param id  车次id
     * @param dto 车次信息
     * @return 车次信息
     */
    @PutMapping("/{id}")
    public TransportTripsDto update(@PathVariable(name = "id") String id, @RequestBody TransportTripsDto dto) {
        PdTransportTrips pdTransportTrips = new PdTransportTrips();
        BeanUtils.copyProperties(dto, pdTransportTrips);
        pdTransportTrips.setId(IdConverter.toLong(id));
        pdTransportTrips.setTransportLineId(IdConverter.toLong(dto.getTransportLineId()));
        transportTripsService.updateById(pdTransportTrips);
        dto.setId(id);
        return dto;
    }

    /**
     * 删除车次信息
     *
     * @param id 车次信息
     * @return 返回信息
     */
    @PutMapping("/{id}/disable")
    public Result disable(@PathVariable(name = "id") String id) {
        transportTripsService.disable(IdConverter.toLong(id));
        return Result.ok();
    }

    /**
     * 批量保存车次与车辆和司机关联关系
     *
     * @param dtoList 车次与车辆和司机关联关系
     * @return 返回信息
     */
    @PostMapping("{id}/truckDriver")
    public Result batchSaveTruckDriver(@PathVariable("id") String transportTripsId,
                                       @RequestBody List<TransportTripsTruckDriverDto> dtoList) {
        Long tripsId = IdConverter.toLong(transportTripsId);
        transportTripsTruckDriverService.batchSave(tripsId, dtoList.stream().map(dto -> {
            PdTransportTripsTruckDriver relation = new PdTransportTripsTruckDriver();
            BeanUtils.copyProperties(dto, relation);
            relation.setTripsId(tripsId);
            relation.setTruckId(IdConverter.toLong(dto.getTruckId()));
            relation.setDriverId(IdConverter.toLong(dto.getUserId()));
            return relation;
        }).toList());
        return Result.ok();
    }

    /**
     * 获取车次与车辆和司机关联关系列表
     *
     * @param transportTripsId 车次id
     * @param truckId          车辆id
     * @param userId           司机id
     * @return 车次与车辆和司机关联关系列表
     */
    @GetMapping("/truckDriver")
    public List<TransportTripsTruckDriverDto> findAllTruckDriverTransportTrips(
            @RequestParam(name = "transportTripsId", required = false) String transportTripsId,
            @RequestParam(name = "truckId", required = false) String truckId,
            @RequestParam(name = "userId", required = false) String userId) {
        return transportTripsTruckDriverService.findAll(IdConverter.toLong(transportTripsId),
                IdConverter.toLong(truckId), IdConverter.toLong(userId)).stream().map(relation -> {
            TransportTripsTruckDriverDto dto = new TransportTripsTruckDriverDto();
            BeanUtils.copyProperties(relation, dto);
            dto.setId(IdConverter.toStr(relation.getId()));
            dto.setTransportTripsId(IdConverter.toStr(relation.getTripsId()));
            dto.setTruckId(IdConverter.toStr(relation.getTruckId()));
            dto.setUserId(IdConverter.toStr(relation.getDriverId()));
            return dto;
        }).toList();
    }

    /**
     * 实体 → DTO：id/线路id 转回 String
     */
    private TransportTripsDto toDto(PdTransportTrips entity) {
        TransportTripsDto dto = new TransportTripsDto();
        BeanUtils.copyProperties(entity, dto);
        dto.setId(IdConverter.toStr(entity.getId()));
        dto.setTransportLineId(IdConverter.toStr(entity.getTransportLineId()));
        return dto;
    }
}
