/**
 * Copyright (c) 2019 联智合创 All rights reserved.
 * <p>
 * http://www.witlinked.com
 * <p>
 * 版权所有，侵权必究！
 */

package com.xinfei.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.xinfei.entity.CacheLineDetailEntity;

import java.util.List;

/**
 * 缓冲线路子表
 *
 * @author
 */
public interface ICacheLineDetailService extends IService<CacheLineDetailEntity> {

    List<CacheLineDetailEntity> findByCacheLineId(Long cacheLineId);
}
