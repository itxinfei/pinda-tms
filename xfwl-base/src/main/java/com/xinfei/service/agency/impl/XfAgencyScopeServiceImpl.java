package com.xinfei.service.agency.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xinfei.entity.agency.XfAgencyScope;
import com.xinfei.mapper.agency.XfAgencyScopeMapper;
import com.xinfei.service.agency.IPdAgencyScopeService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 机构作业范围 服务实现类
 */
@Service
public class XfAgencyScopeServiceImpl extends ServiceImpl<XfAgencyScopeMapper, XfAgencyScope>
        implements IPdAgencyScopeService {

    @Override
    public void batchSave(List<XfAgencyScope> scopeList) {
        saveBatch(scopeList);
    }

    @Override
    public void delete(Integer areaId, Long orgId) {
        LambdaQueryWrapper<XfAgencyScope> wrapper = new LambdaQueryWrapper<>();
        boolean canExecute = false;
        if (areaId != null) {
            wrapper.eq(XfAgencyScope::getAreaId, areaId);
            canExecute = true;
        }
        if (orgId != null) {
            wrapper.eq(XfAgencyScope::getOrgId, orgId);
            canExecute = true;
        }
        if (canExecute) {
            baseMapper.delete(wrapper);
        }
    }

    @Override
    public List<XfAgencyScope> findAll(Integer areaId, Long orgId, List<Long> orgIds, List<Integer> areaIds) {
        LambdaQueryWrapper<XfAgencyScope> wrapper = new LambdaQueryWrapper<>();
        if (areaId != null) {
            wrapper.eq(XfAgencyScope::getAreaId, areaId);
        }
        if (orgId != null) {
            wrapper.eq(XfAgencyScope::getOrgId, orgId);
        }
        if (orgIds != null && !orgIds.isEmpty()) {
            wrapper.in(XfAgencyScope::getOrgId, orgIds);
        }
        if (areaIds != null && !areaIds.isEmpty()) {
            wrapper.in(XfAgencyScope::getAreaId, areaIds);
        }
        return baseMapper.selectList(wrapper);
    }
}
