package com.itheima.pinda.service.user.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.itheima.pinda.entity.user.PdTruckDriver;
import com.itheima.pinda.mapper.user.PdTruckDriverMapper;
import com.itheima.pinda.service.user.IPdTruckDriverService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 司机 服务实现类
 */
@Service
public class PdTruckDriverServiceImpl extends ServiceImpl<PdTruckDriverMapper, PdTruckDriver>
        implements IPdTruckDriverService {

    @Override
    public PdTruckDriver saveTruckDriver(PdTruckDriver pdTruckDriver) {
        PdTruckDriver existing = baseMapper.selectOne(new LambdaQueryWrapper<PdTruckDriver>()
                .eq(PdTruckDriver::getUserId, pdTruckDriver.getUserId()));
        if (existing != null) {
            pdTruckDriver.setId(existing.getId());
        }
        saveOrUpdate(pdTruckDriver);
        return pdTruckDriver;
    }

    @Override
    public List<PdTruckDriver> findAll(List<Long> userIds, Long fleetId) {
        boolean hasUserIds = userIds != null && !userIds.isEmpty();
        if (!hasUserIds && fleetId == null) {
            return new ArrayList<>();
        }
        LambdaQueryWrapper<PdTruckDriver> wrapper = new LambdaQueryWrapper<>();
        if (hasUserIds) {
            wrapper.in(PdTruckDriver::getUserId, userIds);
        }
        if (fleetId != null) {
            wrapper.eq(PdTruckDriver::getFleetId, fleetId);
        }
        wrapper.orderBy(true, false, PdTruckDriver::getId);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public PdTruckDriver findOne(Long userId) {
        if (userId == null) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<PdTruckDriver>()
                .eq(PdTruckDriver::getUserId, userId));
    }

    @Override
    public Integer count(Long fleetId) {
        LambdaQueryWrapper<PdTruckDriver> wrapper = new LambdaQueryWrapper<>();
        if (fleetId != null) {
            wrapper.eq(PdTruckDriver::getFleetId, fleetId);
        }
        return Math.toIntExact(count(wrapper));
    }

    @Override
    public IPage<PdTruckDriver> findByPage(Integer page, Integer pageSize, Long fleetId) {
        Page<PdTruckDriver> iPage = new Page<>(page, pageSize);
        LambdaQueryWrapper<PdTruckDriver> wrapper = new LambdaQueryWrapper<>();
        if (fleetId != null) {
            wrapper.eq(PdTruckDriver::getFleetId, fleetId);
        }
        wrapper.orderBy(true, false, PdTruckDriver::getId);
        return baseMapper.selectPage(iPage, wrapper);
    }
}
