package com.xinfei.service.carrier;

import com.baomidou.mybatisplus.spring.service.IService;
import com.xinfei.entity.carrier.XfCarrierTruck;

import java.util.List;

/**
 * 外协车辆服务（DSP-2 第一期）
 */
public interface IPdCarrierTruckService extends IService<XfCarrierTruck> {

    /**
     * 挂靠新增/修改（id 空=新增；须校验承运商存在且非黑名单）
     */
    XfCarrierTruck saveTruck(XfCarrierTruck truck, Long operator);

    /**
     * 按承运商查挂靠车辆
     */
    List<XfCarrierTruck> listByCarrierId(Long carrierId);
}
