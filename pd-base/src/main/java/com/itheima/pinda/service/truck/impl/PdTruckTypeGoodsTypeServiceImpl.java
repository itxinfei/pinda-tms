package com.itheima.pinda.service.truck.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.itheima.pinda.entity.truck.PdTruckTypeGoodsType;
import com.itheima.pinda.mapper.truck.PdTruckTypeGoodsTypeMapper;
import com.itheima.pinda.service.truck.IPdTruckTypeGoodsTypeService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 车辆类型-货物类型 关联 服务实现类
 */
@Service
public class PdTruckTypeGoodsTypeServiceImpl extends ServiceImpl<PdTruckTypeGoodsTypeMapper, PdTruckTypeGoodsType>
        implements IPdTruckTypeGoodsTypeService {

    @Override
    public void saveTruckTypeGoodsType(PdTruckTypeGoodsType pdTruckTypeGoodsType) {
        baseMapper.insert(pdTruckTypeGoodsType);
    }

    @Override
    public void batchSave(List<PdTruckTypeGoodsType> truckTypeGoodsTypeList) {
        saveBatch(truckTypeGoodsTypeList);
    }

    @Override
    public void delete(Long truckTypeId, Long goodsTypeId) {
        LambdaQueryWrapper<PdTruckTypeGoodsType> wrapper = new LambdaQueryWrapper<>();
        boolean canExecute = false;
        if (truckTypeId != null) {
            wrapper.eq(PdTruckTypeGoodsType::getTruckTypeId, truckTypeId);
            canExecute = true;
        }
        if (goodsTypeId != null) {
            wrapper.eq(PdTruckTypeGoodsType::getGoodsTypeId, goodsTypeId);
            canExecute = true;
        }
        if (canExecute) {
            baseMapper.delete(wrapper);
        }
    }

    @Override
    public List<PdTruckTypeGoodsType> findAll(Long truckTypeId, Long goodsTypeId) {
        LambdaQueryWrapper<PdTruckTypeGoodsType> wrapper = new LambdaQueryWrapper<>();
        if (truckTypeId != null) {
            wrapper.eq(PdTruckTypeGoodsType::getTruckTypeId, truckTypeId);
        }
        if (goodsTypeId != null) {
            wrapper.eq(PdTruckTypeGoodsType::getGoodsTypeId, goodsTypeId);
        }
        return baseMapper.selectList(wrapper);
    }
}
