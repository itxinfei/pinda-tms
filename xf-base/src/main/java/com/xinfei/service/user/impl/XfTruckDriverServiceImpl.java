package com.xinfei.service.user.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xinfei.entity.user.XfTruckDriver;
import com.xinfei.mapper.user.XfTruckDriverMapper;
import com.xinfei.service.user.IPdTruckDriverService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 司机 服务实现类
 */
@Service
public class XfTruckDriverServiceImpl extends ServiceImpl<XfTruckDriverMapper, XfTruckDriver>
        implements IPdTruckDriverService {

    @Override
    public XfTruckDriver saveTruckDriver(XfTruckDriver xfTruckDriver) {
        XfTruckDriver existing = baseMapper.selectOne(new LambdaQueryWrapper<XfTruckDriver>()
                .eq(XfTruckDriver::getUserId, xfTruckDriver.getUserId()));
        if (existing != null) {
            xfTruckDriver.setId(existing.getId());
        }
        saveOrUpdate(xfTruckDriver);
        return xfTruckDriver;
    }

    @Override
    public List<XfTruckDriver> findAll(List<Long> userIds, Long fleetId) {
        boolean hasUserIds = userIds != null && !userIds.isEmpty();
        if (!hasUserIds && fleetId == null) {
            return new ArrayList<>();
        }
        LambdaQueryWrapper<XfTruckDriver> wrapper = new LambdaQueryWrapper<>();
        if (hasUserIds) {
            wrapper.in(XfTruckDriver::getUserId, userIds);
        }
        if (fleetId != null) {
            wrapper.eq(XfTruckDriver::getFleetId, fleetId);
        }
        wrapper.orderBy(true, false, XfTruckDriver::getId);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public XfTruckDriver findOne(Long userId) {
        if (userId == null) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<XfTruckDriver>()
                .eq(XfTruckDriver::getUserId, userId));
    }

    @Override
    public Integer count(Long fleetId) {
        LambdaQueryWrapper<XfTruckDriver> wrapper = new LambdaQueryWrapper<>();
        if (fleetId != null) {
            wrapper.eq(XfTruckDriver::getFleetId, fleetId);
        }
        return Math.toIntExact(count(wrapper));
    }

    @Override
    public IPage<XfTruckDriver> findByPage(Integer page, Integer pageSize, Long fleetId) {
        Page<XfTruckDriver> iPage = new Page<>(page, pageSize);
        LambdaQueryWrapper<XfTruckDriver> wrapper = new LambdaQueryWrapper<>();
        if (fleetId != null) {
            wrapper.eq(XfTruckDriver::getFleetId, fleetId);
        }
        wrapper.orderBy(true, false, XfTruckDriver::getId);
        return baseMapper.selectPage(iPage, wrapper);
    }
}
