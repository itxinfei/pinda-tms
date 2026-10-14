package com.xinfei.service.user;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.IService;
import com.xinfei.entity.user.XfTruckDriver;

import java.util.List;

/**
 * 司机 服务类
 */
public interface IPdTruckDriverService extends IService<XfTruckDriver> {

    /**
     * 添加司机
     */
    XfTruckDriver saveTruckDriver(XfTruckDriver xfTruckDriver);

    /**
     * 获取司机基本信息列表
     *
     * @param userIds 司机关联账号id列表
     */
    List<XfTruckDriver> findAll(List<Long> userIds, Long fleetId);

    /**
     * 按关联账号id获取司机基本信息
     */
    XfTruckDriver findOne(Long userId);

    /**
     * 统计司机数量
     */
    Integer count(Long fleetId);

    /**
     * 获取司机分页数据
     */
    IPage<XfTruckDriver> findByPage(Integer page, Integer pageSize, Long fleetId);
}
