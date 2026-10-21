package com.xinfei.service.truck.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xinfei.entity.truck.XfTruckTypeGoodsType;
import com.xinfei.mapper.truck.XfTruckTypeGoodsTypeMapper;
import com.xinfei.service.truck.IPdTruckTypeGoodsTypeService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 车辆类型-货物类型 关联 服务实现类
 */
@Service
public class XfTruckTypeGoodsTypeServiceImpl extends ServiceImpl<XfTruckTypeGoodsTypeMapper, XfTruckTypeGoodsType>
        implements IPdTruckTypeGoodsTypeService {

    @Override
    public void saveTruckTypeGoodsType(XfTruckTypeGoodsType xfTruckTypeGoodsType) {
        baseMapper.insert(xfTruckTypeGoodsType);
    }

    @Override
    public void batchSave(List<XfTruckTypeGoodsType> truckTypeGoodsTypeList) {
        saveBatch(truckTypeGoodsTypeList);
    }

    @Override
    public void delete(Long truckTypeId, Long goodsTypeId) {
        LambdaQueryWrapper<XfTruckTypeGoodsType> wrapper = new LambdaQueryWrapper<>();
        boolean canExecute = false;
        if (truckTypeId != null) {
            wrapper.eq(XfTruckTypeGoodsType::getTruckTypeId, truckTypeId);
            canExecute = true;
        }
        if (goodsTypeId != null) {
            wrapper.eq(XfTruckTypeGoodsType::getGoodsTypeId, goodsTypeId);
            canExecute = true;
        }
        if (canExecute) {
            baseMapper.delete(wrapper);
        }
    }

    @Override
    public List<XfTruckTypeGoodsType> findAll(Long truckTypeId, Long goodsTypeId) {
        LambdaQueryWrapper<XfTruckTypeGoodsType> wrapper = new LambdaQueryWrapper<>();
        if (truckTypeId != null) {
            wrapper.eq(XfTruckTypeGoodsType::getTruckTypeId, truckTypeId);
        }
        if (goodsTypeId != null) {
            wrapper.eq(XfTruckTypeGoodsType::getGoodsTypeId, goodsTypeId);
        }
        return baseMapper.selectList(wrapper);
    }
}
