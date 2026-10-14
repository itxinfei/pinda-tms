package com.xinfei.service.transportline;

import com.baomidou.mybatisplus.spring.service.IService;
import com.xinfei.entity.transportline.XfTransportTrips;

import java.util.List;

/**
 * 车次 服务类
 */
public interface IPdTransportTripsService extends IService<XfTransportTrips> {

    /**
     * 添加车次
     */
    XfTransportTrips saveTransportTrips(XfTransportTrips xfTransportTrips);

    /**
     * 获取车次列表
     *
     * @param transportLineId 线路id
     * @param ids             车次id列表
     */
    List<XfTransportTrips> findAll(Long transportLineId, List<Long> ids);

    /**
     * 禁用车次
     */
    void disable(Long id);
}
