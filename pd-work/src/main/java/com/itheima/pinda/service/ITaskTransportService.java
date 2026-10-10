package com.itheima.pinda.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.IService;
import com.itheima.pinda.DTO.TaskTransportDTO;
import com.itheima.pinda.entity.TaskTransport;

import java.util.List;

/**
 * 干线运输任务表 服务类
 */
public interface ITaskTransportService extends IService<TaskTransport> {
    /**
     * 新增运输任务
     *
     * @param taskTransport 运输任务信息
     * @return 运输任务信息
     */
    TaskTransport saveTaskTransport(TaskTransport taskTransport);

    /**
     * 保存运输任务并关联运单（事务保护，保证任务和关联关系原子写入）
     *
     * @param taskTransport     运输任务信息
     * @param transportOrderIds 关联的运单ID列表
     * @return 运输任务信息
     */
    TaskTransport saveWithRelations(TaskTransport taskTransport, List<Long> transportOrderIds);

    /**
     * 更新运输任务并重新关联运单（事务保护，保证更新和关联关系原子写入）
     *
     * @param id                运输任务ID
     * @param dto               运输任务DTO
     * @param transportOrderIds 关联的运单ID列表
     * @return 是否成功
     */
    boolean updateWithRelations(Long id, TaskTransportDTO dto, List<Long> transportOrderIds);

    /**
     * 获取运输任务分页数据
     *
     * @param page     页码
     * @param pageSize 页尺寸
     * @param id       任务id
     * @param status   运输任务状态
     * @return 运输任务分页数据
     */
    IPage<TaskTransport> findByPage(Integer page, Integer pageSize, Long id, Integer status);

    /**
     * 获取运输任务列表
     *
     * @param ids    运输任务id列表
     * @param id     运输任务Id
     * @param status 运输任务状态
     * @param dto    查询条件（truckId 等）
     * @return 运输任务列表
     */
    List<TaskTransport> findAll(List<Long> ids, Long id, Integer status, TaskTransportDTO dto);

    /**
     * 更新运输任务状态 - 发车确认
     *
     * @param id 运输任务ID
     * @return 是否成功
     */
    boolean depart(Long id);

    /**
     * 更新运输任务状态 - 到达确认
     *
     * @param id 运输任务ID
     * @return 是否成功
     */
    boolean arrive(Long id);

    /**
     * 更新运输任务状态 - 交付确认
     *
     * @param id 运输任务ID
     * @return 是否成功
     */
    boolean deliver(Long id);

    /**
     * 运输任务完成后，联动更新运单和订单状态
     *
     * @param id 运输任务ID
     * @return 是否成功
     */
    boolean syncStatusOnComplete(Long id);
}
