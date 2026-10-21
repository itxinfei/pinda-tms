package com.xinfei.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.IService;
import com.xinfei.entity.DriverJob;

import java.util.List;

/**
 * 司机作业单 服务类
 */
public interface IDriverJobService extends IService<DriverJob> {
    /**
     * 新增司机作业单
     *
     * @param driverJob 作业单信息
     * @return 作业单信息
     */
    DriverJob saveDriverJob(DriverJob driverJob);

    /**
     * 获取司机作业单分页数据
     *
     * @param page     页码
     * @param pageSize 页尺寸
     * @param id       作业Id
     * @param driverId 司机id
     * @param status   作业状态
     * @param taskTransportId 运输任务id
     * @return 司机作业单分页数据
     */
    IPage<DriverJob> findByPage(Integer page, Integer pageSize, Long id, Long driverId,
                                Integer status, Long taskTransportId);

    /**
     * 获取司机作业单列表
     *
     * @param ids    司机作业单id列表
     * @param id     司机作业单Id
     * @param status 司机作业单状态
     * @return 司机作业单列表
     */
    List<DriverJob> findAll(List<Long> ids, Long id, Long driverId, Integer status, Long taskTransportId);

    /**
     * 按一批运输任务 id 一次性查询司机作业单（分页列表组装司机信息用，避免逐行查询的 N+1）
     *
     * @param taskTransportIds 运输任务 id 集合（为空直接返回空列表，不打库）
     * @return 命中这些任务的司机作业单列表
     */
    List<DriverJob> findAllByTaskTransportIds(List<Long> taskTransportIds);
}
