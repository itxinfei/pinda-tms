package com.xinfei.service.user.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xinfei.entity.user.XfCourierScope;
import com.xinfei.mapper.user.XfCourierScopeMapper;
import com.xinfei.service.user.IPdCourierScopeService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 快递员作业范围 服务实现类
 */
@Service
public class XfCourierScopeServiceImpl extends ServiceImpl<XfCourierScopeMapper, XfCourierScope>
        implements IPdCourierScopeService {

    @Override
    public void batchSave(List<XfCourierScope> scopeList) {
        saveBatch(scopeList);
    }

    @Override
    public void delete(Integer areaId, Long courierId) {
        LambdaQueryWrapper<XfCourierScope> wrapper = new LambdaQueryWrapper<>();
        boolean canExecute = false;
        if (areaId != null) {
            wrapper.eq(XfCourierScope::getAreaId, areaId);
            canExecute = true;
        }
        if (courierId != null) {
            wrapper.eq(XfCourierScope::getCourierId, courierId);
            canExecute = true;
        }
        if (canExecute) {
            baseMapper.delete(wrapper);
        }
    }

    @Override
    public List<XfCourierScope> findAll(Integer areaId, Long courierId) {
        LambdaQueryWrapper<XfCourierScope> wrapper = new LambdaQueryWrapper<>();
        if (areaId != null) {
            wrapper.eq(XfCourierScope::getAreaId, areaId);
        }
        if (courierId != null) {
            wrapper.eq(XfCourierScope::getCourierId, courierId);
        }
        return baseMapper.selectList(wrapper);
    }
}
