package com.xinfei.service.user;

import com.baomidou.mybatisplus.spring.service.IService;
import com.xinfei.entity.user.XfTruckDriverLicense;

/**
 * 司机驾驶证 服务类
 */
public interface IPdTruckDriverLicenseService extends IService<XfTruckDriverLicense> {

    /**
     * 保存司机驾驶证信息
     */
    XfTruckDriverLicense saveTruckDriverLicense(XfTruckDriverLicense xfTruckDriverLicense);

    /**
     * 按司机id获取驾驶证信息
     *
     * @param driverId 司机 base_truck_driver.id
     */
    XfTruckDriverLicense findOne(Long driverId);
}
