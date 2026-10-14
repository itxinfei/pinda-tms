package com.xinfei.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xinfei.entity.PaymentOrder;
import com.xinfei.mapper.PaymentOrderMapper;
import com.xinfei.service.IPaymentOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 订单支付单 Service 实现
 */
@Slf4j
@Service
public class PaymentOrderServiceImpl extends ServiceImpl<PaymentOrderMapper, PaymentOrder> implements IPaymentOrderService {
}
