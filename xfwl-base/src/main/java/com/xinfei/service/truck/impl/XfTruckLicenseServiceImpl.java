package com.xinfei.service.truck.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xinfei.entity.truck.XfTruck;
import com.xinfei.entity.truck.XfTruckLicense;
import com.xinfei.mapper.truck.XfTruckLicenseMapper;
import com.xinfei.service.truck.IPdTruckLicenseService;
import com.xinfei.service.truck.IPdTruckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 车辆行驶证 服务实现类
 */
@Service
public class XfTruckLicenseServiceImpl extends ServiceImpl<XfTruckLicenseMapper, XfTruckLicense>
        implements IPdTruckLicenseService {

    @Autowired
    private IPdTruckService truckService;

    @Override
    public XfTruckLicense saveTruckLicense(XfTruckLicense xfTruckLicense) {
        if (xfTruckLicense.getId() == null) {
            baseMapper.insert(xfTruckLicense);
            // 回填车辆上的行驶证关联字段
            if (xfTruckLicense.getTruckId() != null) {
                XfTruck truck = truckService.getById(xfTruckLicense.getTruckId());
                if (truck != null) {
                    truck.setTruckLicenseId(xfTruckLicense.getId());
                    truckService.updateById(truck);
                }
            }
        } else {
            baseMapper.updateById(xfTruckLicense);
        }
        return xfTruckLicense;
    }
}
