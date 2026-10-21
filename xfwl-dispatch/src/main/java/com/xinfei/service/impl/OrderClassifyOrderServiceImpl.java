package com.xinfei.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xinfei.entity.OrderClassifyOrderEntity;
import com.xinfei.mapper.OrderClassifyOrderMapper;
import com.xinfei.service.IOrderClassifyOrderService;
import com.xinfei.utils.IdConvert;
import org.springframework.stereotype.Service;

@Service
public class OrderClassifyOrderServiceImpl extends ServiceImpl<OrderClassifyOrderMapper, OrderClassifyOrderEntity> implements IOrderClassifyOrderService {
    @Override
    public Long getClassifyId(String orderId) {
        LambdaQueryWrapper<OrderClassifyOrderEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderClassifyOrderEntity::getOrderId, IdConvert.toLong(orderId));

        OrderClassifyOrderEntity entity = this.baseMapper.selectOne(wrapper);
        if (entity == null) {
            return null;
        }
        return entity.getClassifyId();
    }
}
