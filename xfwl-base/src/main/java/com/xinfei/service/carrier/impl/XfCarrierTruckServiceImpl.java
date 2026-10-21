package com.xinfei.service.carrier.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xinfei.entity.carrier.XfCarrier;
import com.xinfei.entity.carrier.XfCarrierTruck;
import com.xinfei.mapper.carrier.XfCarrierTruckMapper;
import com.xinfei.service.carrier.IPdCarrierService;
import com.xinfei.service.carrier.IPdCarrierTruckService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 外协车辆服务实现（DSP-2 第一期）
 */
@Slf4j
@Service
public class XfCarrierTruckServiceImpl extends ServiceImpl<XfCarrierTruckMapper, XfCarrierTruck>
        implements IPdCarrierTruckService {

    @Autowired
    @Lazy
    private IPdCarrierService carrierService;

    @Override
    public XfCarrierTruck saveTruck(XfCarrierTruck truck, Long operator) {
        if (truck.getCarrierId() == null) {
            throw new IllegalArgumentException("承运商ID不能为空（外协车辆必须挂靠承运商）");
        }
        if (StringUtils.isBlank(truck.getLicensePlate())) {
            throw new IllegalArgumentException("车牌号不能为空");
        }
        XfCarrier carrier = carrierService.getById(truck.getCarrierId());
        if (carrier == null) {
            throw new IllegalArgumentException("承运商不存在：" + truck.getCarrierId());
        }
        if (carrier.getStatus() == XfCarrier.STATUS_BLACKLIST) {
            throw new IllegalArgumentException("黑名单承运商禁止挂靠车辆");
        }
        LocalDateTime now = LocalDateTime.now();
        if (truck.getId() == null) {
            truck.setStatus(truck.getStatus() == null ? XfCarrierTruck.STATUS_NORMAL : truck.getStatus());
            truck.setCreateBy(operator);
            truck.setCreateTime(now);
            save(truck);
        } else {
            XfCarrierTruck exists = getById(truck.getId());
            if (exists == null) {
                throw new IllegalArgumentException("外协车辆不存在：" + truck.getId());
            }
            truck.setUpdateBy(operator);
            truck.setUpdateTime(now);
            updateById(truck);
        }
        return truck;
    }

    @Override
    public List<XfCarrierTruck> listByCarrierId(Long carrierId) {
        return lambdaQuery()
                .eq(XfCarrierTruck::getCarrierId, carrierId)
                .orderByDesc(XfCarrierTruck::getCreateTime)
                .list();
    }
}
