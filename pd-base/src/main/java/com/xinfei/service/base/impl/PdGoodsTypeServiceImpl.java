package com.xinfei.service.base.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xinfei.common.utils.Constant;
import com.xinfei.entity.base.PdGoodsType;
import com.xinfei.mapper.base.PdGoodsTypeMapper;
import com.xinfei.service.base.IPdGoodsTypeService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 货物类型管理服务实现
 */
@Service
public class PdGoodsTypeServiceImpl extends ServiceImpl<PdGoodsTypeMapper, PdGoodsType>
        implements IPdGoodsTypeService {

    @Override
    public PdGoodsType saveGoodsType(PdGoodsType pdGoodsType) {
        baseMapper.insert(pdGoodsType);
        return pdGoodsType;
    }

    @Override
    public List<PdGoodsType> findAll() {
        LambdaQueryWrapper<PdGoodsType> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PdGoodsType::getStatus, Constant.DATA_DEFAULT_STATUS);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public IPage<PdGoodsType> findByPage(Integer page, Integer pageSize, String name, Long truckTypeId,
                                         String truckTypeName, Integer status) {
        Page<PdGoodsType> pdPage = new Page<>(page, pageSize);
        pdPage.addOrder(OrderItem.asc("id"));
        pdPage.setRecords(baseMapper.findByPage(pdPage, name, truckTypeId, truckTypeName, status));
        return pdPage;
    }

    @Override
    public List<PdGoodsType> findAll(List<Long> ids) {
        LambdaQueryWrapper<PdGoodsType> wrapper = new LambdaQueryWrapper<>();
        if (ids != null && !ids.isEmpty()) {
            wrapper.in(PdGoodsType::getId, ids);
        }
        return baseMapper.selectList(wrapper);
    }
}
