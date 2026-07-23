package com.itheima.pinda.service.truck;

import java.math.BigDecimal;
import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.pinda.entity.truck.PdTruckType;
import com.itheima.pinda.entity.truck.PdTruckTypeGoodsType;

/**
 * <p>
 * 车辆类型表 服务类
 * </p>
 *
 * @author itcast
 * @since 2019-12-20
 */
public interface IPdTruckTypeService extends IService<PdTruckType> {
    /**
     * 添加车辆类型
     *
     * @param pdTruckType 车辆类型信息
     * @return 车辆类型信息
     */
    PdTruckType saveTruckType(PdTruckType pdTruckType);

    /**
     * 保存车辆类型及关联的货物类型（事务保证）
     *
     * @param pdTruckType  车辆类型信息
     * @param goodsTypeIds 货物类型ID列表
     * @return 车辆类型信息
     */
    PdTruckType saveTruckTypeWithGoodsTypes(PdTruckType pdTruckType, List<String> goodsTypeIds);

    /**
     * 更新车辆类型及关联的货物类型（事务保证）
     *
     * @param truckType   车辆类型信息
     * @param goodsTypeIds 货物类型ID列表
     */
    void updateTruckTypeWithGoodsTypes(PdTruckType truckType, List<String> goodsTypeIds);

    /**
     * 获取车辆类型分页数据
     *
     * @param page            页码
     * @param pageSize        页尺寸
     * @param name            类型名称
     * @param allowableLoad   车型载重
     * @param allowableVolume 车型体积
     * @return 线路类型分页数据
     */
    IPage<PdTruckType> findByPage(Integer page, Integer pageSize, String name, BigDecimal allowableLoad,
                                  BigDecimal allowableVolume);

    /**
     * 获取车辆类型列表
     * @return 车辆类型列表
     */
    List<PdTruckType> findAll(List<String> ids);
}
