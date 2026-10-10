package com.itheima.pinda.service.truck;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.IService;
import com.itheima.pinda.entity.truck.PdTruckType;

import java.math.BigDecimal;
import java.util.List;

/**
 * 车辆类型 服务类
 */
public interface IPdTruckTypeService extends IService<PdTruckType> {

    /**
     * 添加车辆类型
     */
    PdTruckType saveTruckType(PdTruckType pdTruckType);

    /**
     * 保存车辆类型及关联的货物类型（事务保证）
     */
    PdTruckType saveTruckTypeWithGoodsTypes(PdTruckType pdTruckType, List<Long> goodsTypeIds);

    /**
     * 更新车辆类型及关联的货物类型（事务保证）
     */
    void updateTruckTypeWithGoodsTypes(PdTruckType truckType, List<Long> goodsTypeIds);

    /**
     * 获取车辆类型分页数据
     */
    IPage<PdTruckType> findByPage(Integer page, Integer pageSize, String name, BigDecimal allowableLoad,
                                  BigDecimal allowableVolume);

    /**
     * 获取车辆类型列表
     */
    List<PdTruckType> findAll(List<Long> ids);
}
