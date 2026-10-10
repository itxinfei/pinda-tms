package com.itheima.pinda.service.transportline.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.itheima.pinda.common.utils.Constant;
import com.itheima.pinda.entity.transportline.PdTransportLineType;
import com.itheima.pinda.mapper.transportline.PdTransportLineTypeMapper;
import com.itheima.pinda.service.transportline.IPdTransportLineTypeService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 线路类型 服务实现类
 */
@Service
public class PdTransportLineTypeServiceImpl extends ServiceImpl<PdTransportLineTypeMapper, PdTransportLineType>
        implements IPdTransportLineTypeService {

    @Override
    public PdTransportLineType saveTransportLineType(PdTransportLineType pdTransportLineType) {
        baseMapper.insert(pdTransportLineType);
        return pdTransportLineType;
    }

    @Override
    public IPage<PdTransportLineType> findByPage(Integer page, Integer pageSize, String typeNumber, String name,
                                                 Integer agencyType) {
        Page<PdTransportLineType> iPage = new Page<>(page, pageSize);
        LambdaQueryWrapper<PdTransportLineType> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotEmpty(name)) {
            wrapper.like(PdTransportLineType::getName, name);
        }
        if (StringUtils.isNotEmpty(typeNumber)) {
            wrapper.like(PdTransportLineType::getTypeNumber, typeNumber);
        }
        if (agencyType != null) {
            wrapper.and(i -> i.eq(PdTransportLineType::getStartAgencyType, agencyType).or()
                    .eq(PdTransportLineType::getEndAgencyType, agencyType));
        }
        wrapper.eq(PdTransportLineType::getStatus, Constant.DATA_DEFAULT_STATUS);
        wrapper.orderBy(true, true, PdTransportLineType::getId);
        return baseMapper.selectPage(iPage, wrapper);
    }

    @Override
    public List<PdTransportLineType> findAll(List<Long> ids) {
        LambdaQueryWrapper<PdTransportLineType> wrapper = new LambdaQueryWrapper<>();
        if (ids != null && !ids.isEmpty()) {
            wrapper.in(PdTransportLineType::getId, ids);
        }
        wrapper.orderBy(true, true, PdTransportLineType::getId);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public void disableById(Long id) {
        PdTransportLineType lineType = new PdTransportLineType();
        lineType.setId(id);
        lineType.setStatus(Constant.DATA_DISABLE_STATUS);
        baseMapper.updateById(lineType);
    }
}
