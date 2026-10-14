package com.xinfei.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.IService;
import com.xinfei.DTO.OrderDTO;
import com.xinfei.DTO.OrderSearchDTO;
import com.xinfei.entity.Order;
import java.util.List;
import java.util.Map;
/**
 * 订单
 */
public interface IOrderService extends IService<Order> {
    /**
     * 新增订单
     *
     * @param order 订单信息
     * @return 订单信息
     */
    Order saveOrder(Order order);

    /**
     * 获取订单分页数据
     *
     * @param page     页码
     * @param pageSize 页尺寸
     * @param order    查询条件
     * @return 订单分页数据
     */
    IPage<Order> findByPage(Integer page, Integer pageSize, Order order);

    /**
     * 获取订单列表
     *
     * @param ids 订单id列表
     * @return 订单列表
     */
    List<Order> findAll(List<Long> ids);

    /**
     * 获取订单分页数据 客户端使用
     *
     * @return 订单分页数据
     */
    IPage<Order> pageLikeForCustomer(OrderSearchDTO orderSearchDTO);

    /**
     * 计算订单价格
     * @param orderDTO
     * @return
     */
    public Map calculateAmount(OrderDTO orderDTO);

    /**
     * 取消订单：置为已取消(12)并记录取消原因/时间。
     * 状态流转合法性复用 {@link #updateById(Order)} 内嵌矩阵校验，不另写状态机。
     *
     * @param id     订单id
     * @param reason 取消原因
     * @return 是否成功；订单不存在或当前状态不允许取消返回 false
     */
    boolean cancel(Long id, String reason);
}
