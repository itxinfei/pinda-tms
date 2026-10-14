/**
 * Copyright (c) 2019 联智合创 All rights reserved.
 * <p>
 * http://www.witlinked.com
 * <p>
 * 版权所有，侵权必究！
 */

package com.xinfei.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xinfei.entity.OrderClassifyAttachEntity;
import com.xinfei.mapper.OrderClassifyAttachMapper;
import com.xinfei.service.IOrderClassifyAttachService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderClassifyAttachServiceImpl extends ServiceImpl<OrderClassifyAttachMapper, OrderClassifyAttachEntity> implements IOrderClassifyAttachService {

    @Override
    public List<OrderClassifyAttachEntity> findAttachByClassifyId(Long classifyId) {

        LambdaQueryWrapper<OrderClassifyAttachEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderClassifyAttachEntity::getClassifyId, classifyId);

        // 正序排列  从先到后
        wrapper.orderByAsc(OrderClassifyAttachEntity::getCreateTime);
        return this.baseMapper.selectList(wrapper);
    }
}
