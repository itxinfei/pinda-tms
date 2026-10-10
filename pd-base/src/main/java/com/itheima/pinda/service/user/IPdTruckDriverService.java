package com.itheima.pinda.service.user;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.IService;
import com.itheima.pinda.entity.user.PdTruckDriver;

import java.util.List;

/**
 * 司机 服务类
 */
public interface IPdTruckDriverService extends IService<PdTruckDriver> {

    /**
     * 添加司机
     */
    PdTruckDriver saveTruckDriver(PdTruckDriver pdTruckDriver);

    /**
     * 获取司机基本信息列表
     *
     * @param userIds 司机关联账号id列表
     */
    List<PdTruckDriver> findAll(List<Long> userIds, Long fleetId);

    /**
     * 按关联账号id获取司机基本信息
     */
    PdTruckDriver findOne(Long userId);

    /**
     * 统计司机数量
     */
    Integer count(Long fleetId);

    /**
     * 获取司机分页数据
     */
    IPage<PdTruckDriver> findByPage(Integer page, Integer pageSize, Long fleetId);
}
