package com.xinfei.service.carrier.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xinfei.entity.carrier.XfCarrier;
import com.xinfei.entity.carrier.XfCarrierTruck;
import com.xinfei.mapper.carrier.XfCarrierMapper;
import com.xinfei.service.carrier.IPdCarrierService;
import com.xinfei.service.carrier.IPdCarrierTruckService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 承运商服务实现（DSP-2 第一期）
 */
@Slf4j
@Service
public class XfCarrierServiceImpl extends ServiceImpl<XfCarrierMapper, XfCarrier>
        implements IPdCarrierService {

    @Autowired
    private IPdCarrierTruckService carrierTruckService;

    @Override
    public XfCarrier saveCarrier(XfCarrier carrier, Long operator) {
        if (StringUtils.isBlank(carrier.getCode())) {
            throw new IllegalArgumentException("承运商编码不能为空");
        }
        if (StringUtils.isBlank(carrier.getName())) {
            throw new IllegalArgumentException("承运商名称不能为空");
        }
        if (carrier.getSettleType() == null) {
            carrier.setSettleType(XfCarrier.SETTLE_CASH);
        }
        // code 租户内唯一（业务层校验，uk 兜底；逻辑删除后允许复用）
        Long dup = lambdaQuery()
                .eq(XfCarrier::getCode, carrier.getCode())
                .ne(carrier.getId() != null, XfCarrier::getId, carrier.getId())
                .count();
        if (dup != null && dup > 0) {
            throw new IllegalArgumentException("承运商编码已存在：" + carrier.getCode());
        }
        LocalDateTime now = LocalDateTime.now();
        if (carrier.getId() == null) {
            carrier.setStatus(carrier.getStatus() == null ? XfCarrier.STATUS_ACTIVE : carrier.getStatus());
            carrier.setCreateBy(operator);
            carrier.setCreateTime(now);
            save(carrier);
        } else {
            XfCarrier exists = getById(carrier.getId());
            if (exists == null) {
                throw new IllegalArgumentException("承运商不存在：" + carrier.getId());
            }
            carrier.setUpdateBy(operator);
            carrier.setUpdateTime(now);
            updateById(carrier);
        }
        return carrier;
    }

    @Override
    public IPage<XfCarrier> page(Integer page, Integer pageSize, String code, String name, Integer status) {
        int p = page == null || page < 1 ? 1 : page;
        int ps = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        LambdaQueryWrapper<XfCarrier> wrapper = new LambdaQueryWrapper<XfCarrier>()
                .eq(StringUtils.isNotBlank(code), XfCarrier::getCode, code)
                .like(StringUtils.isNotBlank(name), XfCarrier::getName, name)
                .eq(status != null, XfCarrier::getStatus, status)
                .orderByDesc(XfCarrier::getCreateTime);
        return page(new Page<>(p, ps), wrapper);
    }

    @Override
    public boolean changeStatus(Long id, Integer status, Long operator) {
        if (status == null
                || (status != XfCarrier.STATUS_ACTIVE
                && status != XfCarrier.STATUS_SUSPENDED
                && status != XfCarrier.STATUS_BLACKLIST)) {
            throw new IllegalArgumentException("非法状态：" + status);
        }
        XfCarrier exists = getById(id);
        if (exists == null) {
            throw new IllegalArgumentException("承运商不存在：" + id);
        }
        XfCarrier update = new XfCarrier();
        update.setId(id);
        update.setStatus(status);
        update.setUpdateBy(operator);
        update.setUpdateTime(LocalDateTime.now());
        return updateById(update);
    }

    @Override
    public List<Map<String, Object>> capacity(String keyword, Long truckTypeId, BigDecimal minLoad) {
        // 合作中的承运商
        List<XfCarrier> carriers = lambdaQuery()
                .eq(XfCarrier::getStatus, XfCarrier.STATUS_ACTIVE)
                .and(StringUtils.isNotBlank(keyword), w -> w
                        .like(XfCarrier::getName, keyword)
                        .or().like(XfCarrier::getCode, keyword))
                .list();
        if (carriers.isEmpty()) {
            return List.of();
        }
        Map<Long, XfCarrier> carrierMap = carriers.stream()
                .collect(Collectors.toMap(XfCarrier::getId, c -> c, (a, b) -> a, LinkedHashMap::new));
        // 正常状态挂靠车辆（黑名单/暂停承运商的车辆不进池）
        List<XfCarrierTruck> trucks = carrierTruckService.lambdaQuery()
                .in(XfCarrierTruck::getCarrierId, carrierMap.keySet())
                .eq(XfCarrierTruck::getStatus, XfCarrierTruck.STATUS_NORMAL)
                .eq(truckTypeId != null, XfCarrierTruck::getTruckTypeId, truckTypeId)
                .ge(minLoad != null, XfCarrierTruck::getAllowableLoad, minLoad)
                .like(StringUtils.isNotBlank(keyword), XfCarrierTruck::getLicensePlate, keyword)
                .list();
        return trucks.stream().map(t -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", t.getId());
            row.put("carrierId", t.getCarrierId());
            row.put("carrierName", carrierMap.containsKey(t.getCarrierId())
                    ? carrierMap.get(t.getCarrierId()).getName() : null);
            row.put("licensePlate", t.getLicensePlate());
            row.put("truckTypeId", t.getTruckTypeId());
            row.put("allowableLoad", t.getAllowableLoad());
            row.put("allowableVolume", t.getAllowableVolume());
            row.put("status", t.getStatus());
            return row;
        }).collect(Collectors.toList());
    }
}
