package com.itheima.pinda.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.pinda.entity.PaymentOrder;
import com.itheima.pinda.mapper.PaymentOrderMapper;
import com.itheima.pinda.service.IPaymentOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 订单支付单 Service 实现
 */
@Slf4j
@Service
public class PaymentOrderServiceImpl extends ServiceImpl<PaymentOrderMapper, PaymentOrder> implements IPaymentOrderService {
}
