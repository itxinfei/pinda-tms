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
import com.xinfei.entity.CacheLineDetailEntity;
import com.xinfei.mapper.CacheLineDetailMapper;
import com.xinfei.service.ICacheLineDetailService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CacheLineDetailServiceImpl extends ServiceImpl<CacheLineDetailMapper, CacheLineDetailEntity> implements ICacheLineDetailService {

    @Override
    public List<CacheLineDetailEntity> findByCacheLineId(Long cacheLineId) {
        LambdaQueryWrapper<CacheLineDetailEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CacheLineDetailEntity::getCacheLineId, cacheLineId);
        return this.baseMapper.selectList(wrapper);
    }
}
