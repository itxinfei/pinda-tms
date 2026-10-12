package com.itheima.pinda.service.agency.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.itheima.pinda.common.utils.Constant;
import com.itheima.pinda.entity.agency.PdFleet;
import com.itheima.pinda.mapper.agency.PdFleetMapper;
import com.itheima.pinda.service.agency.IPdFleetService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 车队 服务实现类
 */
@Service
public class PdFleetServiceImpl extends ServiceImpl<PdFleetMapper, PdFleet> implements IPdFleetService {

    @Override
    public PdFleet saveFleet(PdFleet fleet) {
        baseMapper.insert(fleet);
        return fleet;
    }

    @Override
    public IPage<PdFleet> findByPage(Integer page, Integer pageSize, String name, String fleetNumber, String manager,
                                     Integer status) {
        Page<PdFleet> iPage = new Page<>(page, pageSize);
        LambdaQueryWrapper<PdFleet> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotEmpty(name)) {
            wrapper.like(PdFleet::getName, name);
        }
        if (StringUtils.isNotEmpty(fleetNumber)) {
            wrapper.like(PdFleet::getFleetNumber, fleetNumber);
        }
        if (StringUtils.isNotEmpty(manager)) {
            wrapper.eq(PdFleet::getManager, manager);
        }
        // status 为 null 时不过滤，档案列表可同时查看启用与停用项
        if (status != null) {
            wrapper.eq(PdFleet::getStatus, status);
        }
        wrapper.orderBy(true, true, PdFleet::getId);
        return baseMapper.selectPage(iPage, wrapper);
    }

    @Override
    public List<PdFleet> findAll(List<Long> ids, Long orgId) {
        LambdaQueryWrapper<PdFleet> wrapper = new LambdaQueryWrapper<>();
        if (ids != null && !ids.isEmpty()) {
            wrapper.in(PdFleet::getId, ids);
        }
        if (orgId != null) {
            wrapper.eq(PdFleet::getOrgId, orgId);
        }
        wrapper.eq(PdFleet::getStatus, Constant.DATA_DEFAULT_STATUS);
        wrapper.orderBy(true, true, PdFleet::getId);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public void disableById(Long id) {
        // 仅更新 status 列：fleetNumber/manager 配了 FieldStrategy.ALWAYS，用实体 updateById 会把未赋值字段一并置 null
        lambdaUpdate().set(PdFleet::getStatus, Constant.DATA_DISABLE_STATUS)
                .eq(PdFleet::getId, id).update();
    }

    @Override
    public void enableById(Long id) {
        // 仅更新 status 列，理由同 disableById
        lambdaUpdate().set(PdFleet::getStatus, Constant.DATA_DEFAULT_STATUS)
                .eq(PdFleet::getId, id).update();
    }
}
