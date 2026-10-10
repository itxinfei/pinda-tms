package com.itheima.pinda.service.agency.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.itheima.pinda.entity.agency.PdAgencyScope;
import com.itheima.pinda.mapper.agency.PdAgencyScopeMapper;
import com.itheima.pinda.service.agency.IPdAgencyScopeService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 机构作业范围 服务实现类
 */
@Service
public class PdAgencyScopeServiceImpl extends ServiceImpl<PdAgencyScopeMapper, PdAgencyScope>
        implements IPdAgencyScopeService {

    @Override
    public void batchSave(List<PdAgencyScope> scopeList) {
        saveBatch(scopeList);
    }

    @Override
    public void delete(Integer areaId, Long orgId) {
        LambdaQueryWrapper<PdAgencyScope> wrapper = new LambdaQueryWrapper<>();
        boolean canExecute = false;
        if (areaId != null) {
            wrapper.eq(PdAgencyScope::getAreaId, areaId);
            canExecute = true;
        }
        if (orgId != null) {
            wrapper.eq(PdAgencyScope::getOrgId, orgId);
            canExecute = true;
        }
        if (canExecute) {
            baseMapper.delete(wrapper);
        }
    }

    @Override
    public List<PdAgencyScope> findAll(Integer areaId, Long orgId, List<Long> orgIds, List<Integer> areaIds) {
        LambdaQueryWrapper<PdAgencyScope> wrapper = new LambdaQueryWrapper<>();
        if (areaId != null) {
            wrapper.eq(PdAgencyScope::getAreaId, areaId);
        }
        if (orgId != null) {
            wrapper.eq(PdAgencyScope::getOrgId, orgId);
        }
        if (orgIds != null && !orgIds.isEmpty()) {
            wrapper.in(PdAgencyScope::getOrgId, orgIds);
        }
        if (areaIds != null && !areaIds.isEmpty()) {
            wrapper.in(PdAgencyScope::getAreaId, areaIds);
        }
        return baseMapper.selectList(wrapper);
    }
}
