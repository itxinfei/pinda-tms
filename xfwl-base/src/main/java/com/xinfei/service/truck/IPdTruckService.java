package com.xinfei.service.truck;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.IService;
import com.xinfei.entity.truck.XfTruck;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 车辆 服务类
 */
public interface IPdTruckService extends IService<XfTruck> {

    /**
     * 添加车辆
     */
    XfTruck saveTruck(XfTruck xfTruck);

    /**
     * 获取车辆分页数据
     */
    IPage<XfTruck> findByPage(Integer page, Integer pageSize, Long truckTypeId, String licensePlate, Long fleetId);

    /**
     * 按多个车队ID获取车辆分页数据（车队名称查询匹配多个车队时使用）
     */
    IPage<XfTruck> findByPageByFleetIds(Integer page, Integer pageSize, Long truckTypeId, String licensePlate,
                                        List<Long> fleetIds);

    /**
     * 获取车辆列表
     */
    List<XfTruck> findAll(List<Long> ids, Long fleetId);

    /**
     * 统计车辆数量
     */
    Integer count(Long fleetId);

    /**
     * 禁用车辆
     */
    void disableById(Long id);

    /**
     * 更新车辆在线状态与心跳时间
     *
     * @param truckId       车辆主键 id
     * @param heartbeatTime 心跳时间
     * @return 是否更新成功
     */
    boolean updateHeartbeat(Long truckId, LocalDateTime heartbeatTime);

    /**
     * 批量将心跳超时车辆置为离线
     *
     * @param threshold 心跳超时阈值
     * @return 受影响行数
     */
    int markOfflineByHeartbeat(LocalDateTime threshold);
}
