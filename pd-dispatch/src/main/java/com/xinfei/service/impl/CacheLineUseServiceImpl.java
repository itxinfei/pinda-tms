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
import com.xinfei.entity.CacheLineUseEntity;
import com.xinfei.mapper.CacheLineUseMapper;
import com.xinfei.service.ICacheLineUseService;
import org.springframework.stereotype.Service;

@Service
public class CacheLineUseServiceImpl extends ServiceImpl<CacheLineUseMapper, CacheLineUseEntity> implements ICacheLineUseService {

    @Override
    public CacheLineUseEntity getByClassifyId(Long classifyId) {
        LambdaQueryWrapper<CacheLineUseEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CacheLineUseEntity::getClassifyId, classifyId);
        return this.baseMapper.selectOne(wrapper);
    }
}
