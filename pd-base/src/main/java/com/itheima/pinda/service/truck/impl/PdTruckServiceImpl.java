package com.itheima.pinda.service.truck.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.itheima.pinda.common.utils.Constant;
import com.itheima.pinda.entity.truck.PdTruck;
import com.itheima.pinda.mapper.truck.PdTruckMapper;
import com.itheima.pinda.service.truck.IPdTruckService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 车辆 服务实现类
 */
@Service
public class PdTruckServiceImpl extends ServiceImpl<PdTruckMapper, PdTruck> implements IPdTruckService {

    @Override
    public PdTruck saveTruck(PdTruck pdTruck) {
        baseMapper.insert(pdTruck);
        return pdTruck;
    }

    @Override
    public IPage<PdTruck> findByPage(Integer page, Integer pageSize, Long truckTypeId, String licensePlate,
                                     Long fleetId) {
        Page<PdTruck> iPage = new Page<>(page, pageSize);
        LambdaQueryWrapper<PdTruck> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotEmpty(licensePlate)) {
            wrapper.like(PdTruck::getLicensePlate, licensePlate);
        }
        if (truckTypeId != null) {
            wrapper.eq(PdTruck::getTruckTypeId, truckTypeId);
        }
        if (fleetId != null) {
            wrapper.eq(PdTruck::getFleetId, fleetId);
        }
        wrapper.eq(PdTruck::getStatus, Constant.DATA_DEFAULT_STATUS);
        wrapper.orderBy(true, false, PdTruck::getId);
        return baseMapper.selectPage(iPage, wrapper);
    }

    @Override
    public IPage<PdTruck> findByPageByFleetIds(Integer page, Integer pageSize, Long truckTypeId, String licensePlate,
                                               List<Long> fleetIds) {
        Page<PdTruck> iPage = new Page<>(page, pageSize);
        // 车队ID列表为空（名称未匹配到任何车队）时直接返回空页
        if (fleetIds == null || fleetIds.isEmpty()) {
            return iPage;
        }
        LambdaQueryWrapper<PdTruck> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotEmpty(licensePlate)) {
            wrapper.like(PdTruck::getLicensePlate, licensePlate);
        }
        if (truckTypeId != null) {
            wrapper.eq(PdTruck::getTruckTypeId, truckTypeId);
        }
        wrapper.in(PdTruck::getFleetId, fleetIds);
        wrapper.eq(PdTruck::getStatus, Constant.DATA_DEFAULT_STATUS);
        wrapper.orderBy(true, false, PdTruck::getId);
        return baseMapper.selectPage(iPage, wrapper);
    }

    @Override
    public List<PdTruck> findAll(List<Long> ids, Long fleetId) {
        LambdaQueryWrapper<PdTruck> wrapper = new LambdaQueryWrapper<>();
        if (ids != null && !ids.isEmpty()) {
            wrapper.in(PdTruck::getId, ids);
        }
        if (fleetId != null) {
            wrapper.eq(PdTruck::getFleetId, fleetId);
        }
        wrapper.eq(PdTruck::getStatus, Constant.DATA_DEFAULT_STATUS);
        wrapper.orderBy(true, false, PdTruck::getId);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public Integer count(Long fleetId) {
        LambdaQueryWrapper<PdTruck> wrapper = new LambdaQueryWrapper<>();
        if (fleetId != null) {
            wrapper.eq(PdTruck::getFleetId, fleetId);
        }
        wrapper.eq(PdTruck::getStatus, Constant.DATA_DEFAULT_STATUS);
        return Math.toIntExact(baseMapper.selectCount(wrapper));
    }

    @Override
    public void disableById(Long id) {
        PdTruck truck = new PdTruck();
        truck.setId(id);
        truck.setStatus(Constant.DATA_DISABLE_STATUS);
        baseMapper.updateById(truck);
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
