package com.xinfei.service.truck;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.IService;
import com.xinfei.entity.truck.XfTruckType;

import java.math.BigDecimal;
import java.util.List;

/**
 * 车辆类型 服务类
 */
public interface IPdTruckTypeService extends IService<XfTruckType> {

    /**
     * 添加车辆类型
     */
    XfTruckType saveTruckType(XfTruckType xfTruckType);

    /**
     * 保存车辆类型及关联的货物类型（事务保证）
     */
    XfTruckType saveTruckTypeWithGoodsTypes(XfTruckType xfTruckType, List<Long> goodsTypeIds);

    /**
     * 更新车辆类型及关联的货物类型（事务保证）
     */
    void updateTruckTypeWithGoodsTypes(XfTruckType truckType, List<Long> goodsTypeIds);

    /**
     * 获取车辆类型分页数据
     *
     * @param status 状态过滤：1=启用 0=停用，null=不过滤（查全部）
     */
    IPage<XfTruckType> findByPage(Integer page, Integer pageSize, String name, BigDecimal allowableLoad,
                                  BigDecimal allowableVolume, Integer status);

    /**
     * 获取车辆类型列表
     */
    List<XfTruckType> findAll(List<Long> ids);
}
