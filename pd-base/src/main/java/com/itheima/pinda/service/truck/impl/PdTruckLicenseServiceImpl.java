package com.itheima.pinda.service.truck.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.itheima.pinda.entity.truck.PdTruck;
import com.itheima.pinda.entity.truck.PdTruckLicense;
import com.itheima.pinda.mapper.truck.PdTruckLicenseMapper;
import com.itheima.pinda.service.truck.IPdTruckLicenseService;
import com.itheima.pinda.service.truck.IPdTruckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 车辆行驶证 服务实现类
 */
@Service
public class PdTruckLicenseServiceImpl extends ServiceImpl<PdTruckLicenseMapper, PdTruckLicense>
        implements IPdTruckLicenseService {

    @Autowired
    private IPdTruckService truckService;

    @Override
    public PdTruckLicense saveTruckLicense(PdTruckLicense pdTruckLicense) {
        if (pdTruckLicense.getId() == null) {
            baseMapper.insert(pdTruckLicense);
            // 回填车辆上的行驶证关联字段
            if (pdTruckLicense.getTruckId() != null) {
                PdTruck truck = truckService.getById(pdTruckLicense.getTruckId());
                if (truck != null) {
                    truck.setTruckLicenseId(pdTruckLicense.getId());
                    truckService.updateById(truck);
                }
            }
        } else {
            baseMapper.updateById(pdTruckLicense);
        }
        return pdTruckLicense;
    }
}
