package com.xinfei.service.agency.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xinfei.common.utils.Constant;
import com.xinfei.entity.agency.XfFleet;
import com.xinfei.mapper.agency.XfFleetMapper;
import com.xinfei.service.agency.IPdFleetService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 车队 服务实现类
 */
@Service
public class XfFleetServiceImpl extends ServiceImpl<XfFleetMapper, XfFleet> implements IPdFleetService {

    @Override
    public XfFleet saveFleet(XfFleet fleet) {
        baseMapper.insert(fleet);
        return fleet;
    }

    @Override
    public IPage<XfFleet> findByPage(Integer page, Integer pageSize, String name, String fleetNumber, String manager,
                                     Integer status) {
        Page<XfFleet> iPage = new Page<>(page, pageSize);
        LambdaQueryWrapper<XfFleet> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotEmpty(name)) {
            wrapper.like(XfFleet::getName, name);
        }
        if (StringUtils.isNotEmpty(fleetNumber)) {
            wrapper.like(XfFleet::getFleetNumber, fleetNumber);
        }
        if (StringUtils.isNotEmpty(manager)) {
            wrapper.eq(XfFleet::getManager, manager);
        }
        // status 为 null 时不过滤，档案列表可同时查看启用与停用项
        if (status != null) {
            wrapper.eq(XfFleet::getStatus, status);
        }
        wrapper.orderBy(true, true, XfFleet::getId);
        return baseMapper.selectPage(iPage, wrapper);
    }

    @Override
    public List<XfFleet> findAll(List<Long> ids, Long orgId) {
        LambdaQueryWrapper<XfFleet> wrapper = new LambdaQueryWrapper<>();
        if (ids != null && !ids.isEmpty()) {
            wrapper.in(XfFleet::getId, ids);
        }
        if (orgId != null) {
            wrapper.eq(XfFleet::getOrgId, orgId);
        }
        wrapper.eq(XfFleet::getStatus, Constant.DATA_DEFAULT_STATUS);
        wrapper.orderBy(true, true, XfFleet::getId);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public void disableById(Long id) {
        // 仅更新 status 列：fleetNumber/manager 配了 FieldStrategy.ALWAYS，用实体 updateById 会把未赋值字段一并置 null
        lambdaUpdate().set(XfFleet::getStatus, Constant.DATA_DISABLE_STATUS)
                .eq(XfFleet::getId, id).update();
    }

    @Override
    public void enableById(Long id) {
        // 仅更新 status 列，理由同 disableById
        lambdaUpdate().set(XfFleet::getStatus, Constant.DATA_DEFAULT_STATUS)
                .eq(XfFleet::getId, id).update();
    }
}
