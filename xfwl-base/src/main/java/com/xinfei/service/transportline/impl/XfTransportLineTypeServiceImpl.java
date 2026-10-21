package com.xinfei.service.transportline.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xinfei.common.utils.Constant;
import com.xinfei.entity.transportline.XfTransportLineType;
import com.xinfei.mapper.transportline.XfTransportLineTypeMapper;
import com.xinfei.service.transportline.IPdTransportLineTypeService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 线路类型 服务实现类
 */
@Service
public class XfTransportLineTypeServiceImpl extends ServiceImpl<XfTransportLineTypeMapper, XfTransportLineType>
        implements IPdTransportLineTypeService {

    @Override
    public XfTransportLineType saveTransportLineType(XfTransportLineType xfTransportLineType) {
        baseMapper.insert(xfTransportLineType);
        return xfTransportLineType;
    }

    @Override
    public IPage<XfTransportLineType> findByPage(Integer page, Integer pageSize, String typeNumber, String name,
                                                 Integer agencyType, Integer status) {
        Page<XfTransportLineType> iPage = new Page<>(page, pageSize);
        LambdaQueryWrapper<XfTransportLineType> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotEmpty(name)) {
            wrapper.like(XfTransportLineType::getName, name);
        }
        if (StringUtils.isNotEmpty(typeNumber)) {
            wrapper.like(XfTransportLineType::getTypeNumber, typeNumber);
        }
        if (agencyType != null) {
            wrapper.and(i -> i.eq(XfTransportLineType::getStartAgencyType, agencyType).or()
                    .eq(XfTransportLineType::getEndAgencyType, agencyType));
        }
        // status 为 null 时不过滤，档案列表可同时查看启用与停用项
        if (status != null) {
            wrapper.eq(XfTransportLineType::getStatus, status);
        }
        wrapper.orderBy(true, true, XfTransportLineType::getId);
        return baseMapper.selectPage(iPage, wrapper);
    }

    @Override
    public List<XfTransportLineType> findAll(List<Long> ids) {
        LambdaQueryWrapper<XfTransportLineType> wrapper = new LambdaQueryWrapper<>();
        if (ids != null && !ids.isEmpty()) {
            wrapper.in(XfTransportLineType::getId, ids);
        }
        wrapper.orderBy(true, true, XfTransportLineType::getId);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public void disableById(Long id) {
        XfTransportLineType lineType = new XfTransportLineType();
        lineType.setId(id);
        lineType.setStatus(Constant.DATA_DISABLE_STATUS);
        baseMapper.updateById(lineType);
    }

    @Override
    public void enableById(Long id) {
        XfTransportLineType lineType = new XfTransportLineType();
        lineType.setId(id);
        lineType.setStatus(Constant.DATA_DEFAULT_STATUS);
        baseMapper.updateById(lineType);
    }
}
