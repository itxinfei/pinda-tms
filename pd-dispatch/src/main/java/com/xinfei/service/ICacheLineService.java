/**
 * Copyright (c) 2019 联智合创 All rights reserved.
 * <p>
 * http://www.witlinked.com
 * <p>
 * 版权所有，侵权必究！
 */

package com.xinfei.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.xinfei.entity.CacheLineEntity;

/**
 * 缓冲线路
 *
 * @author
 */
public interface ICacheLineService extends IService<CacheLineEntity> {

    /**
     * 按校验 key 查当前生效线路ID，不存在返回 null
     */
    Long check(String verifyKey);

    int saveAndUpdate(CacheLineEntity cacheLineEntity);

    /**
     * 作废两机构间的当前版本线路，返回新版本号
     */
    Integer deleteOldAndGetNewVersion(Long notEqualsLineStart, Long notEqualsLineEnd);
}
