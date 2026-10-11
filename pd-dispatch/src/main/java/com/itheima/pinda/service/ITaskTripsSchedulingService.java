package com.itheima.pinda.service;

import com.itheima.pinda.DTO.OrderLineSimpleDTO;
import com.itheima.pinda.DTO.OrderLineTripsTruckDriverDTO;

import java.util.List;

/**
 * 车辆调度
 */
public interface ITaskTripsSchedulingService {
    /**
     * 执行
     *
     * @param orderLineSimpleDTOS 线路简单DTO集合
     * @param businessId          机构ID
     * @param jobId               定时任务id
     * @param logId               日志id
     * @param agencyId            机构ID
     * @return
     */
    List<OrderLineTripsTruckDriverDTO> execute(List<OrderLineSimpleDTO> orderLineSimpleDTOS, Long businessId, Long jobId, Long logId, Long agencyId);
}
