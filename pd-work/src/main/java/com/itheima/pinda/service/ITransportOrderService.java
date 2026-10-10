package com.itheima.pinda.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.IService;
import com.itheima.pinda.entity.TransportOrder;

import java.util.List;

/**
 * 运单表 服务类
 */
public interface ITransportOrderService extends IService<TransportOrder> {
    /**
     * 新增运单
     *
     * @param transportOrder 运单信息
     * @return 运单信息
     */
    TransportOrder saveTransportOrder(TransportOrder transportOrder);

    /**
     * 获取运单分页数据
     *
     * @param page             页码
     * @param pageSize         页尺寸
     * @param orderId          订单Id
     * @param status           运单状态
     * @param schedulingStatus 运单调度状态
     * @return 运单分页数据
     */
    IPage<TransportOrder> findByPage(Integer page, Integer pageSize, Long orderId, Integer status,
                                     Integer schedulingStatus);

    /**
     * 获取运单列表
     *
     * @param ids              运单id列表
     * @param orderId          订单Id
     * @param status           运单状态
     * @param schedulingStatus 运单调度状态
     * @return 运单列表
     */
    List<TransportOrder> findAll(List<Long> ids, Long orderId, Integer status, Integer schedulingStatus);

    /**
     * 通过订单id获取运单信息
     *
     * @param orderId 订单id
     * @return 运单信息
     */
    TransportOrder findByOrderId(Long orderId);
}
