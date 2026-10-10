package com.itheima.pinda.service.transportline;

import com.baomidou.mybatisplus.spring.service.IService;
import com.itheima.pinda.entity.transportline.PdTransportTrips;

import java.util.List;

/**
 * 车次 服务类
 */
public interface IPdTransportTripsService extends IService<PdTransportTrips> {

    /**
     * 添加车次
     */
    PdTransportTrips saveTransportTrips(PdTransportTrips pdTransportTrips);

    /**
     * 获取车次列表
     *
     * @param transportLineId 线路id
     * @param ids             车次id列表
     */
    List<PdTransportTrips> findAll(Long transportLineId, List<Long> ids);

    /**
     * 禁用车次
     */
    void disable(Long id);
}
