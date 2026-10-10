package com.itheima.pinda.service.truck;

import com.baomidou.mybatisplus.spring.service.IService;
import com.itheima.pinda.entity.truck.PdTruckTypeGoodsType;

import java.util.List;

/**
 * 车辆类型-货物类型 关联 服务类
 */
public interface IPdTruckTypeGoodsTypeService extends IService<PdTruckTypeGoodsType> {

    /**
     * 添加车辆类型与货物类型关联
     */
    void saveTruckTypeGoodsType(PdTruckTypeGoodsType pdTruckTypeGoodsType);

    /**
     * 批量添加车辆类型与货物类型关联
     */
    void batchSave(List<PdTruckTypeGoodsType> truckTypeGoodsTypeList);

    /**
     * 删除关联关系
     */
    void delete(Long truckTypeId, Long goodsTypeId);

    /**
     * 获取车辆类型与货物类型关联
     */
    List<PdTruckTypeGoodsType> findAll(Long truckTypeId, Long goodsTypeId);
}
