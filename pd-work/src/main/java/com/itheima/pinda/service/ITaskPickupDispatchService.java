package com.itheima.pinda.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.IService;
import com.itheima.pinda.entity.TaskPickupDispatch;

import java.util.List;

/**
 * 取件、派件任务信息表 服务类
 */
public interface ITaskPickupDispatchService extends IService<TaskPickupDispatch> {
    /**
     * 新增取派件任务
     *
     * @param taskPickupDispatch 取派件任务信息
     * @return 取派件任务信息
     */
    TaskPickupDispatch saveTaskPickupDispatch(TaskPickupDispatch taskPickupDispatch);

    /**
     * 获取取派件任务分页数据
     *
     * @param page     页码
     * @param pageSize 页尺寸
     * @param dispatch 查询条件
     * @return 取派件任务分页数据
     */
    IPage<TaskPickupDispatch> findByPage(Integer page, Integer pageSize, TaskPickupDispatch dispatch);

    /**
     * 获取取派件任务列表
     *
     * @param ids      取派件任务id列表
     * @param orderIds 订单id列表
     * @param dispatch 查询条件
     * @return 取派件任务列表
     */
    List<TaskPickupDispatch> findAll(List<Long> ids, List<Long> orderIds, TaskPickupDispatch dispatch);
}
