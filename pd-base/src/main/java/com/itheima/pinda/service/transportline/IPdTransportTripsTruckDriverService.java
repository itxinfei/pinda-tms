package com.itheima.pinda.service.transportline;

import com.baomidou.mybatisplus.spring.service.IService;
import com.itheima.pinda.entity.transportline.PdTransportTripsTruckDriver;

import java.util.List;

/**
 * 车次-车辆-司机 关联 服务类
 */
public interface IPdTransportTripsTruckDriverService extends IService<PdTransportTripsTruckDriver> {

    /**
     * 批量保存车次与车辆、司机关联
     *
     * @param tripsId               车次id
     * @param truckTransportTrips 关联信息列表
     */
    void batchSave(Long tripsId, List<PdTransportTripsTruckDriver> truckTransportTrips);

    /**
     * 获取车次与车辆、司机关联列表
     *
     * @param tripsId 车次id
     * @param truckId 车辆id
     * @param driverId 司机id
     */
    List<PdTransportTripsTruckDriver> findAll(Long tripsId, Long truckId, Long driverId);
}
