package com.xinfei.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.xinfei.entity.OrderClassifyOrderEntity;

/**
 * 订单分类关联订单
 */
public interface IOrderClassifyOrderService extends IService<OrderClassifyOrderEntity> {

    /**
     * 按订单ID（pd-oms String 边界）查分类ID
     */
    Long getClassifyId(String orderId);
}
