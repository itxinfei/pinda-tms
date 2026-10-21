package com.xinfei.service.transportline.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xinfei.entity.transportline.XfTransportTripsTruckDriver;
import com.xinfei.mapper.transportline.XfTransportTripsTruckDriverMapper;
import com.xinfei.service.transportline.IPdTransportTripsTruckDriverService;
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
public class XfTransportTripsTruckDriverServiceImpl
        extends ServiceImpl<XfTransportTripsTruckDriverMapper, XfTransportTripsTruckDriver>
        implements IPdTransportTripsTruckDriverService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchSave(Long tripsId, List<XfTransportTripsTruckDriver> sourceList) {
        LambdaQueryWrapper<XfTransportTripsTruckDriver> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(XfTransportTripsTruckDriver::getTripsId, tripsId);
        // 查出操作前关系列表
        List<XfTransportTripsTruckDriver> existingList = baseMapper.selectList(wrapper);
        Map<String, XfTransportTripsTruckDriver> sourceKeyMap = new HashMap<>();
        for (XfTransportTripsTruckDriver existing : existingList) {
            sourceKeyMap.put(existing.getTripsId() + "_" + existing.getTruckId(), existing);
        }
        // 清除旧关系
        baseMapper.delete(wrapper);
        List<XfTransportTripsTruckDriver> saveList = new ArrayList<>();
        sourceList.forEach(source -> {
            XfTransportTripsTruckDriver saveData = new XfTransportTripsTruckDriver();
            BeanUtils.copyProperties(source, saveData);
            String key = source.getTripsId() + "_" + source.getTruckId();
            XfTransportTripsTruckDriver old = sourceKeyMap.get(key);
            if (old != null && saveData.getDriverId() == null) {
                saveData.setDriverId(old.getDriverId());
            }
            saveList.add(saveData);
        });
        saveBatch(saveList);
    }

    @Override
    public List<XfTransportTripsTruckDriver> findAll(Long tripsId, Long truckId, Long driverId) {
        LambdaQueryWrapper<XfTransportTripsTruckDriver> wrapper = new LambdaQueryWrapper<>();
        if (tripsId != null) {
            wrapper.eq(XfTransportTripsTruckDriver::getTripsId, tripsId);
        }
        if (truckId != null) {
            wrapper.eq(XfTransportTripsTruckDriver::getTruckId, truckId);
        }
        if (driverId != null) {
            wrapper.eq(XfTransportTripsTruckDriver::getDriverId, driverId);
        }
        return baseMapper.selectList(wrapper);
    }
}
