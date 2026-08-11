package com.itheima.pinda.service.truck;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.pinda.entity.truck.PdTruck;

import java.util.List;

/**
 * <p>
 * 车辆信息表 服务类
 * </p>
 *
 * @author itcast
 * @since 2019-12-20
 */
public interface IPdTruckService extends IService<PdTruck> {
    /**
     * 添加车辆
     *
     * @param pdTruck 车辆信息
     * @return 车辆信息
     */
    PdTruck saveTruck(PdTruck pdTruck);

    /**
     * 获取车辆分页数据
     *
     * @param page         页码
     * @param pageSize     页尺寸
     * @param truckTypeId  车辆类型id
     * @param licensePlate 车辆号牌
     * @return 线路类型分页数据
     */
    IPage<PdTruck> findByPage(Integer page, Integer pageSize, String truckTypeId, String licensePlate, String fleetId);

    /**
     * 按多个车队ID获取车辆分页数据（支持车队名称查询时一个名称匹配多个车队）
     *
     * @param page         页码
     * @param pageSize     页尺寸
     * @param truckTypeId  车辆类型id
     * @param licensePlate 车辆号牌
     * @param fleetIds     车队ID列表
     * @return 车辆分页数据
     */
    IPage<PdTruck> findByPageByFleetIds(Integer page, Integer pageSize, String truckTypeId, String licensePlate, List<String> fleetIds);

    /**
     * 获取车辆列表
     *
     * @param ids     车辆id列表
     * @param fleetId 车队id
     * @return 车辆列表
     */
    List<PdTruck> findAll(List<String> ids, String fleetId);

    /**
     * 统计车辆数量
     *
     * @param fleetId 车队id
     * @return 车辆数量
     */
    Integer count(String fleetId);

    /**
     * 删除车辆
     *
     * @param id
     */
    void disableById(String id);
}
