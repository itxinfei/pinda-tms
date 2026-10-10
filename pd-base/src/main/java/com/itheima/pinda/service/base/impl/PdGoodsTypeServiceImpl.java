package com.itheima.pinda.service.base.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.itheima.pinda.common.utils.Constant;
import com.itheima.pinda.entity.base.PdGoodsType;
import com.itheima.pinda.mapper.base.PdGoodsTypeMapper;
import com.itheima.pinda.service.base.IPdGoodsTypeService;
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
                                         String truckTypeName) {
        Page<PdGoodsType> pdPage = new Page<>(page, pageSize);
        pdPage.addOrder(OrderItem.asc("id"));
        pdPage.setRecords(baseMapper.findByPage(pdPage, name, truckTypeId, truckTypeName));
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
