package com.itheima.pinda.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.pinda.entity.ScheduleExceptionOrder;

import java.util.List;

/**
 * 异常调度订单 Service
 */
public interface IScheduleExceptionOrderService extends IService<ScheduleExceptionOrder> {

    /**
     * 登记无法调度的订单（幂等：同一订单存在待处理记录时不重复登记）
     *
     * @param orderId  订单ID
     * @param agencyId 当前机构ID
     * @param reason   异常原因
     * @return 是否新增登记
     */
    boolean registerExceptionOrder(String orderId, String agencyId, String reason);

    /**
     * 查询所有待处理的异常订单（用于自动重试调度）
     *
     * @return 待处理异常订单列表
     */
    List<ScheduleExceptionOrder> listPending();

    /**
     * 标记异常订单为已处理（重试成功后调用）
     *
     * @param id     记录ID
     * @param remark 处理备注
     * @return 是否成功
     */
    boolean markHandled(String id, String remark);
}
