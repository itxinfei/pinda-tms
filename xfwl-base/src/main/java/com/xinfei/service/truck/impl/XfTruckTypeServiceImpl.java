package com.xinfei.service.truck.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xinfei.common.utils.Constant;
import com.xinfei.entity.truck.XfTruckType;
import com.xinfei.entity.truck.XfTruckTypeGoodsType;
import com.xinfei.mapper.truck.XfTruckTypeMapper;
import com.xinfei.service.truck.IPdTruckTypeGoodsTypeService;
import com.xinfei.service.truck.IPdTruckTypeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 车辆类型 服务实现类
 */
@Service
public class XfTruckTypeServiceImpl extends ServiceImpl<XfTruckTypeMapper, XfTruckType>
        implements IPdTruckTypeService {

    private final IPdTruckTypeGoodsTypeService truckTypeGoodsTypeService;

    public XfTruckTypeServiceImpl(IPdTruckTypeGoodsTypeService truckTypeGoodsTypeService) {
        this.truckTypeGoodsTypeService = truckTypeGoodsTypeService;
    }

    @Override
    public XfTruckType saveTruckType(XfTruckType xfTruckType) {
        baseMapper.insert(xfTruckType);
        return xfTruckType;
    }

    @Override
    public IPage<XfTruckType> findByPage(Integer page, Integer pageSize, String name, BigDecimal allowableLoad,
                                         BigDecimal allowableVolume, Integer status) {
        Page<XfTruckType> iPage = new Page<>(page, pageSize);
        LambdaQueryWrapper<XfTruckType> wrapper = new LambdaQueryWrapper<>();
        if (name != null && !name.isEmpty()) {
            wrapper.like(XfTruckType::getName, name);
        }
        if (allowableLoad != null) {
            wrapper.eq(XfTruckType::getAllowableLoad, allowableLoad);
        }
        if (allowableVolume != null) {
            wrapper.eq(XfTruckType::getAllowableVolume, allowableVolume);
        }
        // status 为 null 时不过滤，档案列表可同时查看启用与停用项
        if (status != null) {
            wrapper.eq(XfTruckType::getStatus, status);
        }
        return baseMapper.selectPage(iPage, wrapper);
    }

    @Override
    public List<XfTruckType> findAll(List<Long> ids) {
        LambdaQueryWrapper<XfTruckType> wrapper = new LambdaQueryWrapper<>();
        if (ids != null && !ids.isEmpty()) {
            wrapper.in(XfTruckType::getId, ids);
        }
        wrapper.eq(XfTruckType::getStatus, Constant.DATA_DEFAULT_STATUS);
        return baseMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public XfTruckType saveTruckTypeWithGoodsTypes(XfTruckType xfTruckType, List<Long> goodsTypeIds) {
        if (xfTruckType.getStatus() == null) {
            xfTruckType.setStatus(Constant.DATA_DEFAULT_STATUS);
        }
        baseMapper.insert(xfTruckType);
        saveGoodsRelations(xfTruckType.getId(), goodsTypeIds);
        return xfTruckType;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTruckTypeWithGoodsTypes(XfTruckType truckType, List<Long> goodsTypeIds) {
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
        List<XfTruckTypeGoodsType> list = goodsTypeIds.stream().map(goodsTypeId -> {
            XfTruckTypeGoodsType item = new XfTruckTypeGoodsType();
            item.setGoodsTypeId(goodsTypeId);
            item.setTruckTypeId(truckTypeId);
            return item;
        }).collect(Collectors.toList());
        truckTypeGoodsTypeService.batchSave(list);
    }
}
