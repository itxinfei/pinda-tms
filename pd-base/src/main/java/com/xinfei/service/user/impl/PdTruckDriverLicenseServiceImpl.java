package com.xinfei.service.user.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xinfei.entity.user.PdTruckDriverLicense;
import com.xinfei.mapper.user.PdTruckDriverLicenseMapper;
import com.xinfei.service.user.IPdTruckDriverLicenseService;
import org.springframework.stereotype.Service;

/**
 * 司机驾驶证 服务实现类
 */
@Service
public class PdTruckDriverLicenseServiceImpl
        extends ServiceImpl<PdTruckDriverLicenseMapper, PdTruckDriverLicense>
        implements IPdTruckDriverLicenseService {

    @Override
    public PdTruckDriverLicense saveTruckDriverLicense(PdTruckDriverLicense pdTruckDriverLicense) {
        PdTruckDriverLicense existing = baseMapper.selectOne(new LambdaQueryWrapper<PdTruckDriverLicense>()
                .eq(PdTruckDriverLicense::getDriverId, pdTruckDriverLicense.getDriverId()));
        if (existing != null) {
            pdTruckDriverLicense.setId(existing.getId());
        }
        saveOrUpdate(pdTruckDriverLicense);
        return pdTruckDriverLicense;
    }

    @Override
    public PdTruckDriverLicense findOne(Long driverId) {
        if (driverId == null) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<PdTruckDriverLicense>()
                .eq(PdTruckDriverLicense::getDriverId, driverId));
    }
}
