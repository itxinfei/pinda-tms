package com.xinfei.service.transportline.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xinfei.common.utils.Constant;
import com.xinfei.entity.transportline.PdTransportLine;
import com.xinfei.mapper.transportline.PdTransportLineMapper;
import com.xinfei.service.transportline.IPdTransportLineService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 业务线路 服务实现类
 */
@Service
public class PdTransportLineServiceImpl extends ServiceImpl<PdTransportLineMapper, PdTransportLine>
        implements IPdTransportLineService {

    @Override
    public PdTransportLine saveTransportLine(PdTransportLine pdTransportLine) {
        baseMapper.insert(pdTransportLine);
        return pdTransportLine;
    }

    @Override
    public IPage<PdTransportLine> findByPage(Integer page, Integer pageSize, String lineNumber, String name,
                                             Long transportLineTypeId) {
        Page<PdTransportLine> iPage = new Page<>(page, pageSize);
        LambdaQueryWrapper<PdTransportLine> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotEmpty(name)) {
            wrapper.like(PdTransportLine::getName, name);
        }
        if (StringUtils.isNotEmpty(lineNumber)) {
            wrapper.like(PdTransportLine::getLineNumber, lineNumber);
        }
        if (transportLineTypeId != null) {
            wrapper.eq(PdTransportLine::getTransportLineTypeId, transportLineTypeId);
        }
        wrapper.eq(PdTransportLine::getStatus, Constant.DATA_DEFAULT_STATUS);
        wrapper.orderBy(true, false, PdTransportLine::getId);
        return baseMapper.selectPage(iPage, wrapper);
    }

    @Override
    public List<PdTransportLine> findAll(List<Long> ids, Long orgId, List<Long> orgIds) {
        LambdaQueryWrapper<PdTransportLine> wrapper = new LambdaQueryWrapper<>();
        if (ids != null && !ids.isEmpty()) {
            wrapper.in(PdTransportLine::getId, ids);
        }
        if (orgId != null) {
            wrapper.eq(PdTransportLine::getOrgId, orgId);
        }
        if (orgIds != null && !orgIds.isEmpty()) {
            wrapper.in(PdTransportLine::getOrgId, orgIds);
        }
        wrapper.eq(PdTransportLine::getStatus, Constant.DATA_DEFAULT_STATUS);
        wrapper.orderBy(true, false, PdTransportLine::getId);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public void disable(Long id) {
        // 仅更新 status 列：业务字段配了 FieldStrategy.ALWAYS，用实体 updateById 会把未赋值字段一并置 null
        lambdaUpdate().set(PdTransportLine::getStatus, Constant.DATA_DISABLE_STATUS)
                .eq(PdTransportLine::getId, id).update();
    }
}
