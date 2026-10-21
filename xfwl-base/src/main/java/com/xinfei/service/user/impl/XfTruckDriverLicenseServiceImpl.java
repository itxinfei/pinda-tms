package com.xinfei.service.user.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xinfei.entity.user.XfTruckDriverLicense;
import com.xinfei.mapper.user.XfTruckDriverLicenseMapper;
import com.xinfei.service.user.IPdTruckDriverLicenseService;
import org.springframework.stereotype.Service;

/**
 * 司机驾驶证 服务实现类
 */
@Service
public class XfTruckDriverLicenseServiceImpl
        extends ServiceImpl<XfTruckDriverLicenseMapper, XfTruckDriverLicense>
        implements IPdTruckDriverLicenseService {

    @Override
    public XfTruckDriverLicense saveTruckDriverLicense(XfTruckDriverLicense xfTruckDriverLicense) {
        XfTruckDriverLicense existing = baseMapper.selectOne(new LambdaQueryWrapper<XfTruckDriverLicense>()
                .eq(XfTruckDriverLicense::getDriverId, xfTruckDriverLicense.getDriverId()));
        if (existing != null) {
            xfTruckDriverLicense.setId(existing.getId());
        }
        saveOrUpdate(xfTruckDriverLicense);
        return xfTruckDriverLicense;
    }

    @Override
    public XfTruckDriverLicense findOne(Long driverId) {
        if (driverId == null) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<XfTruckDriverLicense>()
                .eq(XfTruckDriverLicense::getDriverId, driverId));
    }
}
