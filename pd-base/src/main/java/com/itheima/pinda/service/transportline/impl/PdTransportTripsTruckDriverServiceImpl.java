package com.itheima.pinda.service.transportline.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.itheima.pinda.entity.transportline.PdTransportTripsTruckDriver;
import com.itheima.pinda.mapper.transportline.PdTransportTripsTruckDriverMapper;
import com.itheima.pinda.service.transportline.IPdTransportTripsTruckDriverService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 车次-车辆-司机 关联 服务实现类
 */
@Service
public class PdTransportTripsTruckDriverServiceImpl
        extends ServiceImpl<PdTransportTripsTruckDriverMapper, PdTransportTripsTruckDriver>
        implements IPdTransportTripsTruckDriverService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchSave(Long tripsId, List<PdTransportTripsTruckDriver> sourceList) {
        LambdaQueryWrapper<PdTransportTripsTruckDriver> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PdTransportTripsTruckDriver::getTripsId, tripsId);
        // 查出操作前关系列表
        List<PdTransportTripsTruckDriver> existingList = baseMapper.selectList(wrapper);
        Map<String, PdTransportTripsTruckDriver> sourceKeyMap = new HashMap<>();
        for (PdTransportTripsTruckDriver existing : existingList) {
            sourceKeyMap.put(existing.getTripsId() + "_" + existing.getTruckId(), existing);
        }
        // 清除旧关系
        baseMapper.delete(wrapper);
        List<PdTransportTripsTruckDriver> saveList = new ArrayList<>();
        sourceList.forEach(source -> {
            PdTransportTripsTruckDriver saveData = new PdTransportTripsTruckDriver();
            BeanUtils.copyProperties(source, saveData);
            String key = source.getTripsId() + "_" + source.getTruckId();
            PdTransportTripsTruckDriver old = sourceKeyMap.get(key);
            if (old != null && saveData.getDriverId() == null) {
                saveData.setDriverId(old.getDriverId());
            }
            saveList.add(saveData);
        });
        saveBatch(saveList);
    }

    @Override
    public List<PdTransportTripsTruckDriver> findAll(Long tripsId, Long truckId, Long driverId) {
        LambdaQueryWrapper<PdTransportTripsTruckDriver> wrapper = new LambdaQueryWrapper<>();
        if (tripsId != null) {
            wrapper.eq(PdTransportTripsTruckDriver::getTripsId, tripsId);
        }
        if (truckId != null) {
            wrapper.eq(PdTransportTripsTruckDriver::getTruckId, truckId);
        }
        if (driverId != null) {
            wrapper.eq(PdTransportTripsTruckDriver::getDriverId, driverId);
        }
        return baseMapper.selectList(wrapper);
    }
}
