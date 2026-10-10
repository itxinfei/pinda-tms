package com.itheima.pinda.service.user.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.itheima.pinda.entity.user.PdCourierScope;
import com.itheima.pinda.mapper.user.PdCourierScopeMapper;
import com.itheima.pinda.service.user.IPdCourierScopeService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 快递员作业范围 服务实现类
 */
@Service
public class PdCourierScopeServiceImpl extends ServiceImpl<PdCourierScopeMapper, PdCourierScope>
        implements IPdCourierScopeService {

    @Override
    public void batchSave(List<PdCourierScope> scopeList) {
        saveBatch(scopeList);
    }

    @Override
    public void delete(Integer areaId, Long courierId) {
        LambdaQueryWrapper<PdCourierScope> wrapper = new LambdaQueryWrapper<>();
        boolean canExecute = false;
        if (areaId != null) {
            wrapper.eq(PdCourierScope::getAreaId, areaId);
            canExecute = true;
        }
        if (courierId != null) {
            wrapper.eq(PdCourierScope::getCourierId, courierId);
            canExecute = true;
        }
        if (canExecute) {
            baseMapper.delete(wrapper);
        }
    }

    @Override
    public List<PdCourierScope> findAll(Integer areaId, Long courierId) {
        LambdaQueryWrapper<PdCourierScope> wrapper = new LambdaQueryWrapper<>();
        if (areaId != null) {
            wrapper.eq(PdCourierScope::getAreaId, areaId);
        }
        if (courierId != null) {
            wrapper.eq(PdCourierScope::getCourierId, courierId);
        }
        return baseMapper.selectList(wrapper);
    }
}
