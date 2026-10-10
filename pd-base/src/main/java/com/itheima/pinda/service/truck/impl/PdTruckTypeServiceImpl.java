package com.itheima.pinda.service.truck.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.itheima.pinda.common.utils.Constant;
import com.itheima.pinda.entity.truck.PdTruckType;
import com.itheima.pinda.entity.truck.PdTruckTypeGoodsType;
import com.itheima.pinda.mapper.truck.PdTruckTypeMapper;
import com.itheima.pinda.service.truck.IPdTruckTypeGoodsTypeService;
import com.itheima.pinda.service.truck.IPdTruckTypeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 车辆类型 服务实现类
 */
@Service
public class PdTruckTypeServiceImpl extends ServiceImpl<PdTruckTypeMapper, PdTruckType>
        implements IPdTruckTypeService {

    private final IPdTruckTypeGoodsTypeService truckTypeGoodsTypeService;

    public PdTruckTypeServiceImpl(IPdTruckTypeGoodsTypeService truckTypeGoodsTypeService) {
        this.truckTypeGoodsTypeService = truckTypeGoodsTypeService;
    }

    @Override
    public PdTruckType saveTruckType(PdTruckType pdTruckType) {
        baseMapper.insert(pdTruckType);
        return pdTruckType;
    }

    @Override
    public IPage<PdTruckType> findByPage(Integer page, Integer pageSize, String name, BigDecimal allowableLoad,
                                         BigDecimal allowableVolume) {
        Page<PdTruckType> iPage = new Page<>(page, pageSize);
        LambdaQueryWrapper<PdTruckType> wrapper = new LambdaQueryWrapper<>();
        if (name != null && !name.isEmpty()) {
            wrapper.like(PdTruckType::getName, name);
        }
        if (allowableLoad != null) {
            wrapper.eq(PdTruckType::getAllowableLoad, allowableLoad);
        }
        if (allowableVolume != null) {
            wrapper.eq(PdTruckType::getAllowableVolume, allowableVolume);
        }
        wrapper.eq(PdTruckType::getStatus, Constant.DATA_DEFAULT_STATUS);
        return baseMapper.selectPage(iPage, wrapper);
    }

    @Override
    public List<PdTruckType> findAll(List<Long> ids) {
        LambdaQueryWrapper<PdTruckType> wrapper = new LambdaQueryWrapper<>();
        if (ids != null && !ids.isEmpty()) {
            wrapper.in(PdTruckType::getId, ids);
        }
        wrapper.eq(PdTruckType::getStatus, Constant.DATA_DEFAULT_STATUS);
        return baseMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PdTruckType saveTruckTypeWithGoodsTypes(PdTruckType pdTruckType, List<Long> goodsTypeIds) {
        if (pdTruckType.getStatus() == null) {
            pdTruckType.setStatus(Constant.DATA_DEFAULT_STATUS);
        }
        baseMapper.insert(pdTruckType);
        saveGoodsRelations(pdTruckType.getId(), goodsTypeIds);
        return pdTruckType;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTruckTypeWithGoodsTypes(PdTruckType truckType, List<Long> goodsTypeIds) {
        baseMapper.updateById(truckType);
        if (goodsTypeIds != null) {
            truckTypeGoodsTypeService.delete(truckType.getId(), null);
            saveGoodsRelations(truckType.getId(), goodsTypeIds);
        }
    }

    private void saveGoodsRelations(Long truckTypeId, List<Long> goodsTypeIds) {
        if (goodsTypeIds == null || goodsTypeIds.isEmpty()) {
            return;
        }
        List<PdTruckTypeGoodsType> list = goodsTypeIds.stream().map(goodsTypeId -> {
            PdTruckTypeGoodsType item = new PdTruckTypeGoodsType();
            item.setGoodsTypeId(goodsTypeId);
            item.setTruckTypeId(truckTypeId);
            return item;
        }).collect(Collectors.toList());
        truckTypeGoodsTypeService.batchSave(list);
    }
}
