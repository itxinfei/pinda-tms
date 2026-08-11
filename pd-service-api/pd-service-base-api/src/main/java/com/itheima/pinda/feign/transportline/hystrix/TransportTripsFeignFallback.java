package com.itheima.pinda.feign.transportline.hystrix;

import com.itheima.pinda.DTO.transportline.TransportTripsDto;
import com.itheima.pinda.DTO.transportline.TransportTripsTruckDriverDto;
import com.itheima.pinda.common.utils.Result;
import com.itheima.pinda.feign.transportline.TransportTripsFeign;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 车次Feign接口熔断降级
 */
@Slf4j
@Component
public class TransportTripsFeignFallback implements TransportTripsFeign {

    @Override
    public TransportTripsDto save(TransportTripsDto dto) {
        log.warn("远程调用 pd-base 失败: save({}), 返回null", dto);
        return null;
    }

    @Override
    public TransportTripsDto fineById(String id) {
        log.warn("远程调用 pd-base 失败: fineById({}), 返回null", id);
        return null;
    }

    @Override
    public List<TransportTripsDto> findAll(String transportLineId, List<String> ids) {
        // 返回 null 表示"无法验证"（而非空列表），便于调用方在删除等关键操作上 fail-closed
        log.warn("远程调用 pd-base 失败: findAll(transportLineId={}, ids={}), 返回null", transportLineId, ids);
        return null;
    }

    @Override
    public TransportTripsDto update(String id, TransportTripsDto dto) {
        log.warn("远程调用 pd-base 失败: update({}), 返回null", id);
        return null;
    }

    @Override
    public Result disable(String id) {
        log.warn("远程调用 pd-base 失败: disable({}), 返回失败结果", id);
        return Result.error("服务降级，disable执行失败");
    }

    @Override
    public Result batchSaveTruckDriver(String transportTripsId, List<TransportTripsTruckDriverDto> dtoList) {
        log.warn("远程调用 pd-base 失败: batchSaveTruckDriver(transportTripsId={}, dtoList={}), 返回失败结果", transportTripsId, dtoList);
        return Result.error("服务降级，batchSaveTruckDriver执行失败");
    }

    @Override
    public List<TransportTripsTruckDriverDto> findAllTruckDriverTransportTrips(String transportTripsId, String truckId, String userId) {
        // 返回 null 表示"无法验证"（而非空列表），便于调用方在删除等关键操作上 fail-closed
        log.warn("远程调用 pd-base 失败: findAllTruckDriverTransportTrips(transportTripsId={}, truckId={}, userId={}), 返回null", transportTripsId, truckId, userId);
        return null;
    }
}
