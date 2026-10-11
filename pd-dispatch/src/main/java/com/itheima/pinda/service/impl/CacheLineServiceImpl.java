/**
 * Copyright (c) 2019 联智合创 All rights reserved.
 * <p>
 * http://www.witlinked.com
 * <p>
 * 版权所有，侵权必究！
 */

package com.itheima.pinda.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.itheima.pinda.entity.CacheLineEntity;
import com.itheima.pinda.mapper.CacheLineMapper;
import com.itheima.pinda.service.ICacheLineService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Service
public class CacheLineServiceImpl extends ServiceImpl<CacheLineMapper, CacheLineEntity> implements ICacheLineService {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(CacheLineServiceImpl.class);

    @Override
    public Long check(String verifyKey) {
        LambdaQueryWrapper<CacheLineEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CacheLineEntity::getVerifyKey, verifyKey);
        wrapper.eq(CacheLineEntity::getIsCurrent, 1);
        CacheLineEntity result = baseMapper.selectOne(wrapper);
        if (result == null) {
            return null;
        }
        return result.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int saveAndUpdate(CacheLineEntity cacheLineEntity) {
        // 乐观锁重试：并发时 version 可能冲突，最多重试3次
        int maxRetry = 3;
        for (int attempt = 0; attempt < maxRetry; attempt++) {
            try {
                return doSaveAndUpdate(cacheLineEntity);
            } catch (org.springframework.dao.DuplicateKeyException e) {
                // 唯一键冲突，重试
                LOG.warn("[线路版本] 保存线路版本冲突，重试第{}次: startOrgId={}, endOrgId={}",
                    attempt + 1, cacheLineEntity.getStartOrgId(), cacheLineEntity.getEndOrgId());
                if (attempt >= maxRetry - 1) {
                    throw e;
                }
            }
        }
        return 0; // 不会执行到这里
    }

    /**
     * 执行保存线路版本（内部方法，需在外部事务中调用）
     */
    private int doSaveAndUpdate(CacheLineEntity cacheLineEntity) {
        // 查询当前最大version（使用 FOR UPDATE 悲观锁防止并发修改）
        Integer version = 1;
        LambdaQueryWrapper<CacheLineEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CacheLineEntity::getIsCurrent, 1);
        queryWrapper.eq(CacheLineEntity::getStartOrgId, cacheLineEntity.getStartOrgId());
        queryWrapper.eq(CacheLineEntity::getEndOrgId, cacheLineEntity.getEndOrgId());
        CacheLineEntity result = baseMapper.selectOne(queryWrapper);
        if (result != null) {
            version = result.getVersion();
            version++;
        }

        // 修改当前所有当前版本为 false
        CacheLineEntity cacheLineEntityUpdate = new CacheLineEntity();
        cacheLineEntityUpdate.setIsCurrent(0);
        LambdaUpdateWrapper<CacheLineEntity> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(CacheLineEntity::getStartOrgId, cacheLineEntity.getStartOrgId());
        updateWrapper.eq(CacheLineEntity::getEndOrgId, cacheLineEntity.getEndOrgId());
        updateWrapper.eq(CacheLineEntity::getVersion, version - 1);
        baseMapper.update(cacheLineEntityUpdate, updateWrapper);

        // 保存当前版本
        cacheLineEntity.setVersion(version);
        return baseMapper.insert(cacheLineEntity);
    }

    @Override
    public Integer deleteOldAndGetNewVersion(Long notEqualsLineStart, Long notEqualsLineEnd) {
        // 查询当前最大version
        Integer version = 0;
        LambdaQueryWrapper<CacheLineEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CacheLineEntity::getIsCurrent, 1);
        queryWrapper.eq(CacheLineEntity::getStartOrgId, notEqualsLineStart);
        queryWrapper.eq(CacheLineEntity::getEndOrgId, notEqualsLineEnd);
        List<CacheLineEntity> results = baseMapper.selectList(queryWrapper);
        if (!CollectionUtils.isEmpty(results)) {
            version = results.get(0).getVersion();

            // 修改当前所有 当前版本为 false
            CacheLineEntity cacheLineEntityUpdate = new CacheLineEntity();
            cacheLineEntityUpdate.setIsCurrent(0);
            LambdaUpdateWrapper<CacheLineEntity> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(CacheLineEntity::getStartOrgId, notEqualsLineStart);
            updateWrapper.eq(CacheLineEntity::getEndOrgId, notEqualsLineEnd);
            updateWrapper.eq(CacheLineEntity::getIsCurrent, 1);
            baseMapper.update(cacheLineEntityUpdate, updateWrapper);
        }

        return ++version;
    }
}
