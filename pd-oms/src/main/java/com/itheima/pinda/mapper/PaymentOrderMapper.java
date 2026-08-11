package com.itheima.pinda.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itheima.pinda.entity.PaymentOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单支付单 Mapper
 */
@Mapper
public interface PaymentOrderMapper extends BaseMapper<PaymentOrder> {
}
