package com.xinfei.service.carrier;

import com.baomidou.mybatisplus.spring.service.IService;
import com.xinfei.entity.carrier.XfCarrierDriver;

import java.util.List;

/**
 * 外协司机服务（DSP-2 第一期）
 */
public interface IPdCarrierDriverService extends IService<XfCarrierDriver> {

    /**
     * 挂靠新增/修改（id 空=新增；须校验承运商存在且非黑名单）
     */
    XfCarrierDriver saveDriver(XfCarrierDriver driver, Long operator);

    /**
     * 按承运商查挂靠司机
     */
    List<XfCarrierDriver> listByCarrierId(Long carrierId);
}
