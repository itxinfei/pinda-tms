package com.xinfei.service.base.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xinfei.common.utils.Constant;
import com.xinfei.entity.base.XfGoodsType;
import com.xinfei.mapper.base.XfGoodsTypeMapper;
import com.xinfei.service.base.IPdGoodsTypeService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 货物类型管理服务实现
 */
@Service
public class XfGoodsTypeServiceImpl extends ServiceImpl<XfGoodsTypeMapper, XfGoodsType>
        implements IPdGoodsTypeService {

    @Override
    public XfGoodsType saveGoodsType(XfGoodsType xfGoodsType) {
        baseMapper.insert(xfGoodsType);
        return xfGoodsType;
    }

    @Override
    public List<XfGoodsType> findAll() {
        LambdaQueryWrapper<XfGoodsType> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(XfGoodsType::getStatus, Constant.DATA_DEFAULT_STATUS);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public IPage<XfGoodsType> findByPage(Integer page, Integer pageSize, String name, Long truckTypeId,
                                         String truckTypeName, Integer status) {
        Page<XfGoodsType> xfPage = new Page<>(page, pageSize);
        xfPage.addOrder(OrderItem.asc("id"));
        xfPage.setRecords(baseMapper.findByPage(xfPage, name, truckTypeId, truckTypeName, status));
        return xfPage;
    }

    @Override
    public List<XfGoodsType> findAll(List<Long> ids) {
        LambdaQueryWrapper<XfGoodsType> wrapper = new LambdaQueryWrapper<>();
        if (ids != null && !ids.isEmpty()) {
            wrapper.in(XfGoodsType::getId, ids);
        }
        return baseMapper.selectList(wrapper);
    }
}
