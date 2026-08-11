package com.itheima.pinda.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.pinda.entity.ScheduleExceptionOrder;

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
}
