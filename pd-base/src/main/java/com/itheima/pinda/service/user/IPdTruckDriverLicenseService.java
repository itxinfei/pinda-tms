package com.itheima.pinda.service.user;

import com.baomidou.mybatisplus.spring.service.IService;
import com.itheima.pinda.entity.user.PdTruckDriverLicense;

/**
 * 司机驾驶证 服务类
 */
public interface IPdTruckDriverLicenseService extends IService<PdTruckDriverLicense> {

    /**
     * 保存司机驾驶证信息
     */
    PdTruckDriverLicense saveTruckDriverLicense(PdTruckDriverLicense pdTruckDriverLicense);

    /**
     * 按司机id获取驾驶证信息
     *
     * @param driverId 司机 base_truck_driver.id
     */
    PdTruckDriverLicense findOne(Long driverId);
}
