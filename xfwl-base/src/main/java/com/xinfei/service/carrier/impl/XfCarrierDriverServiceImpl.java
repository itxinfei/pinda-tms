package com.xinfei.service.carrier.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xinfei.entity.carrier.XfCarrier;
import com.xinfei.entity.carrier.XfCarrierDriver;
import com.xinfei.mapper.carrier.XfCarrierDriverMapper;
import com.xinfei.service.carrier.IPdCarrierDriverService;
import com.xinfei.service.carrier.IPdCarrierService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 外协司机服务实现（DSP-2 第一期）
 */
@Slf4j
@Service
public class XfCarrierDriverServiceImpl extends ServiceImpl<XfCarrierDriverMapper, XfCarrierDriver>
        implements IPdCarrierDriverService {

    @Autowired
    @Lazy
    private IPdCarrierService carrierService;

    @Override
    public XfCarrierDriver saveDriver(XfCarrierDriver driver, Long operator) {
        if (driver.getCarrierId() == null) {
            throw new IllegalArgumentException("承运商ID不能为空（外协司机必须挂靠承运商）");
        }
        if (StringUtils.isBlank(driver.getName())) {
            throw new IllegalArgumentException("司机姓名不能为空");
        }
        XfCarrier carrier = carrierService.getById(driver.getCarrierId());
        if (carrier == null) {
            throw new IllegalArgumentException("承运商不存在：" + driver.getCarrierId());
        }
        if (carrier.getStatus() == XfCarrier.STATUS_BLACKLIST) {
            throw new IllegalArgumentException("黑名单承运商禁止挂靠司机");
        }
        LocalDateTime now = LocalDateTime.now();
        if (driver.getId() == null) {
            driver.setStatus(driver.getStatus() == null ? XfCarrierDriver.STATUS_NORMAL : driver.getStatus());
            driver.setCreateBy(operator);
            driver.setCreateTime(now);
            save(driver);
        } else {
            XfCarrierDriver exists = getById(driver.getId());
            if (exists == null) {
                throw new IllegalArgumentException("外协司机不存在：" + driver.getId());
            }
            driver.setUpdateBy(operator);
            driver.setUpdateTime(now);
            updateById(driver);
        }
        return driver;
    }

    @Override
    public List<XfCarrierDriver> listByCarrierId(Long carrierId) {
        return lambdaQuery()
                .eq(XfCarrierDriver::getCarrierId, carrierId)
                .orderByDesc(XfCarrierDriver::getCreateTime)
                .list();
    }
}
