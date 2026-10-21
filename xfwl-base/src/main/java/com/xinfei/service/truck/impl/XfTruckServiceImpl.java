package com.xinfei.service.truck.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xinfei.common.utils.Constant;
import com.xinfei.entity.truck.XfTruck;
import com.xinfei.mapper.truck.XfTruckMapper;
import com.xinfei.service.truck.IPdTruckService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 车辆 服务实现类
 */
@Service
public class XfTruckServiceImpl extends ServiceImpl<XfTruckMapper, XfTruck> implements IPdTruckService {

    @Override
    public XfTruck saveTruck(XfTruck xfTruck) {
        baseMapper.insert(xfTruck);
        return xfTruck;
    }

    @Override
    public IPage<XfTruck> findByPage(Integer page, Integer pageSize, Long truckTypeId, String licensePlate,
                                     Long fleetId) {
        Page<XfTruck> iPage = new Page<>(page, pageSize);
        LambdaQueryWrapper<XfTruck> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotEmpty(licensePlate)) {
            wrapper.like(XfTruck::getLicensePlate, licensePlate);
        }
        if (truckTypeId != null) {
            wrapper.eq(XfTruck::getTruckTypeId, truckTypeId);
        }
        if (fleetId != null) {
            wrapper.eq(XfTruck::getFleetId, fleetId);
        }
        wrapper.eq(XfTruck::getStatus, Constant.DATA_DEFAULT_STATUS);
        wrapper.orderBy(true, false, XfTruck::getId);
        return baseMapper.selectPage(iPage, wrapper);
    }

    @Override
    public IPage<XfTruck> findByPageByFleetIds(Integer page, Integer pageSize, Long truckTypeId, String licensePlate,
                                               List<Long> fleetIds) {
        Page<XfTruck> iPage = new Page<>(page, pageSize);
        // 车队ID列表为空（名称未匹配到任何车队）时直接返回空页
        if (fleetIds == null || fleetIds.isEmpty()) {
            return iPage;
        }
        LambdaQueryWrapper<XfTruck> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotEmpty(licensePlate)) {
            wrapper.like(XfTruck::getLicensePlate, licensePlate);
        }
        if (truckTypeId != null) {
            wrapper.eq(XfTruck::getTruckTypeId, truckTypeId);
        }
        wrapper.in(XfTruck::getFleetId, fleetIds);
        wrapper.eq(XfTruck::getStatus, Constant.DATA_DEFAULT_STATUS);
        wrapper.orderBy(true, false, XfTruck::getId);
        return baseMapper.selectPage(iPage, wrapper);
    }

    @Override
    public List<XfTruck> findAll(List<Long> ids, Long fleetId) {
        LambdaQueryWrapper<XfTruck> wrapper = new LambdaQueryWrapper<>();
        if (ids != null && !ids.isEmpty()) {
            wrapper.in(XfTruck::getId, ids);
        }
        if (fleetId != null) {
            wrapper.eq(XfTruck::getFleetId, fleetId);
        }
        wrapper.eq(XfTruck::getStatus, Constant.DATA_DEFAULT_STATUS);
        wrapper.orderBy(true, false, XfTruck::getId);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public Integer count(Long fleetId) {
        LambdaQueryWrapper<XfTruck> wrapper = new LambdaQueryWrapper<>();
        if (fleetId != null) {
            wrapper.eq(XfTruck::getFleetId, fleetId);
        }
        wrapper.eq(XfTruck::getStatus, Constant.DATA_DEFAULT_STATUS);
        return Math.toIntExact(baseMapper.selectCount(wrapper));
    }

    @Override
    public void disableById(Long id) {
        // 仅更新 status 列：业务字段配了 FieldStrategy.ALWAYS，用实体 updateById 会把未赋值字段一并置 null
        lambdaUpdate().set(XfTruck::getStatus, Constant.DATA_DISABLE_STATUS)
                .eq(XfTruck::getId, id).update();
    }

    @Override
    public boolean updateHeartbeat(Long truckId, LocalDateTime heartbeatTime) {
        if (truckId == null || heartbeatTime == null) {
            return false;
        }
        return baseMapper.updateHeartbeat(truckId, heartbeatTime) > 0;
    }

    @Override
    public int markOfflineByHeartbeat(LocalDateTime threshold) {
        if (threshold == null) {
            return 0;
        }
        return baseMapper.markOfflineByHeartbeat(threshold);
    }
}
