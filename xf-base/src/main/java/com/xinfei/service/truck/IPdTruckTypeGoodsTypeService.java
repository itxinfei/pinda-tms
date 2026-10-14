package com.xinfei.service.truck;

import com.baomidou.mybatisplus.spring.service.IService;
import com.xinfei.entity.truck.XfTruckTypeGoodsType;

import java.util.List;

/**
 * 车辆类型-货物类型 关联 服务类
 */
public interface IPdTruckTypeGoodsTypeService extends IService<XfTruckTypeGoodsType> {

    /**
     * 添加车辆类型与货物类型关联
     */
    void saveTruckTypeGoodsType(XfTruckTypeGoodsType xfTruckTypeGoodsType);

    /**
     * 批量添加车辆类型与货物类型关联
     */
    void batchSave(List<XfTruckTypeGoodsType> truckTypeGoodsTypeList);

    /**
     * 删除关联关系
     */
    void delete(Long truckTypeId, Long goodsTypeId);

    /**
     * 获取车辆类型与货物类型关联
     */
    List<XfTruckTypeGoodsType> findAll(Long truckTypeId, Long goodsTypeId);
}
