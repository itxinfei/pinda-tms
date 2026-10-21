package com.xinfei.service.carrier;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.IService;
import com.xinfei.entity.carrier.XfCarrier;
import com.xinfei.entity.carrier.XfCarrierTruck;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 承运商服务（DSP-2 外协运力池第一期）
 */
public interface IPdCarrierService extends IService<XfCarrier> {

    /**
     * 新增/修改承运商（id 空=新增；code 租户内唯一）
     */
    XfCarrier saveCarrier(XfCarrier carrier, Long operator);

    /**
     * 分页查询（code 精确、name 模糊、status 筛选）
     */
    IPage<XfCarrier> page(Integer page, Integer pageSize, String code, String name, Integer status);

    /**
     * 状态流转：1合作中 / 0暂停 / 2黑名单（黑名单仅人工判定，红线）
     */
    boolean changeStatus(Long id, Integer status, Long operator);

    /**
     * 运力池查询：车辆 status=1 且所属承运商 status=1（合作中），
     * 按关键词（车牌/承运商名/编码模糊）、车型、最低载重筛选；
     * 返回 Map 列表（车辆字段 + carrierName 便于前端直接展示）。
     */
    List<Map<String, Object>> capacity(String keyword, Long truckTypeId, BigDecimal minLoad);
}
