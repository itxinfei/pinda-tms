package com.itheima.pinda.service.truck.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.pinda.common.CustomIdGenerator;
import com.itheima.pinda.common.utils.Constant;
import com.itheima.pinda.mapper.truck.PdTruckMapper;
import com.itheima.pinda.entity.truck.PdTruck;
import com.itheima.pinda.service.truck.IPdTruckService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.apache.commons.lang.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 车辆信息表 服务实现类
 * </p>
 *
 * @author itcast
 * @since 2019-12-20
 */
@Service
public class PdTruckServiceImpl extends ServiceImpl<PdTruckMapper, PdTruck> implements IPdTruckService {
    @Autowired
    private CustomIdGenerator idGenerator;

    @Override
    public PdTruck saveTruck(PdTruck pdTruck) {
        pdTruck.setId(idGenerator.nextId(pdTruck) + "");
        baseMapper.insert(pdTruck);
        return pdTruck;
    }

    @Override
    public IPage<PdTruck> findByPage(Integer page, Integer pageSize, String truckTypeId, String licensePlate, String fleetId) {
        Page<PdTruck> iPage = new Page(page, pageSize);
        LambdaQueryWrapper<PdTruck> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotEmpty(licensePlate)) {
            lambdaQueryWrapper.like(PdTruck::getLicensePlate, licensePlate);
        }
        if (StringUtils.isNotEmpty(truckTypeId)) {
            lambdaQueryWrapper.eq(PdTruck::getTruckTypeId, truckTypeId);

        }
        if (StringUtils.isNotEmpty(fleetId)) {
            lambdaQueryWrapper.eq(PdTruck::getFleetId, fleetId);

        }
        lambdaQueryWrapper.eq(PdTruck::getStatus, Constant.DATA_DEFAULT_STATUS);
        lambdaQueryWrapper.orderBy(true, false, PdTruck::getId);
        return baseMapper.selectPage(iPage, lambdaQueryWrapper);
    }

    @Override
    public IPage<PdTruck> findByPageByFleetIds(Integer page, Integer pageSize, String truckTypeId, String licensePlate, List<String> fleetIds) {
        Page<PdTruck> iPage = new Page(page, pageSize);
        // 车队ID列表为空（如名称未匹配到任何车队）时直接返回空页，避免误返回全部车辆
        if (fleetIds == null || fleetIds.isEmpty()) {
            return iPage;
        }
        LambdaQueryWrapper<PdTruck> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotEmpty(licensePlate)) {
            lambdaQueryWrapper.like(PdTruck::getLicensePlate, licensePlate);
        }
        if (StringUtils.isNotEmpty(truckTypeId)) {
            lambdaQueryWrapper.eq(PdTruck::getTruckTypeId, truckTypeId);
        }
        lambdaQueryWrapper.in(PdTruck::getFleetId, fleetIds);
        lambdaQueryWrapper.eq(PdTruck::getStatus, Constant.DATA_DEFAULT_STATUS);
        lambdaQueryWrapper.orderBy(true, false, PdTruck::getId);
        return baseMapper.selectPage(iPage, lambdaQueryWrapper);
    }

    @Override
    public List<PdTruck> findAll(List<String> ids, String fleetId) {
        LambdaQueryWrapper<PdTruck> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        if (ids != null && ids.size() > 0) {
            lambdaQueryWrapper.in(PdTruck::getId, ids);
        }
        if (StringUtils.isNotEmpty(fleetId)) {
            lambdaQueryWrapper.eq(PdTruck::getFleetId, fleetId);
        }
        lambdaQueryWrapper.eq(PdTruck::getStatus, Constant.DATA_DEFAULT_STATUS);
        lambdaQueryWrapper.orderBy(true, false, PdTruck::getId);
        return baseMapper.selectList(lambdaQueryWrapper);
    }

    @Override
    public Integer count(String fleetId) {
        LambdaQueryWrapper<PdTruck> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotEmpty(fleetId)) {
            lambdaQueryWrapper.eq(PdTruck::getFleetId, fleetId);
        }
        lambdaQueryWrapper.eq(PdTruck::getStatus, Constant.DATA_DEFAULT_STATUS);
        return baseMapper.selectCount(lambdaQueryWrapper);
    }

    @Override
    public void disableById(String id) {
        PdTruck pdTruck = new PdTruck();
        pdTruck.setId(id);
        pdTruck.setStatus(Constant.DATA_DISABLE_STATUS);
        baseMapper.updateById(pdTruck);
    }

    /**
     * 更新车辆在线状态与心跳时间（参数为车辆主键 id）
     */
    @Override
    public boolean updateHeartbeat(String truckId, LocalDateTime heartbeatTime) {
        if (StringUtils.isEmpty(truckId) || heartbeatTime == null) {
            return false;
        }
        int affected = baseMapper.updateHeartbeat(truckId, heartbeatTime);
        return affected > 0;
    }

    /**
     * 批量将心跳超时车辆置为离线（P0-4 北斗字段最小改造 · D-13）
     */
    @Override
    public int markOfflineByHeartbeat(LocalDateTime threshold) {
        if (threshold == null) {
            return 0;
        }
        return baseMapper.markOfflineByHeartbeat(threshold);
    }

}
