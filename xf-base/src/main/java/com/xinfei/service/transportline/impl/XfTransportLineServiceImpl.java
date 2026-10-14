package com.xinfei.service.transportline.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xinfei.common.utils.Constant;
import com.xinfei.entity.transportline.XfTransportLine;
import com.xinfei.mapper.transportline.XfTransportLineMapper;
import com.xinfei.service.transportline.IPdTransportLineService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 业务线路 服务实现类
 */
@Service
public class XfTransportLineServiceImpl extends ServiceImpl<XfTransportLineMapper, XfTransportLine>
        implements IPdTransportLineService {

    @Override
    public XfTransportLine saveTransportLine(XfTransportLine xfTransportLine) {
        baseMapper.insert(xfTransportLine);
        return xfTransportLine;
    }

    @Override
    public IPage<XfTransportLine> findByPage(Integer page, Integer pageSize, String lineNumber, String name,
                                             Long transportLineTypeId) {
        Page<XfTransportLine> iPage = new Page<>(page, pageSize);
        LambdaQueryWrapper<XfTransportLine> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotEmpty(name)) {
            wrapper.like(XfTransportLine::getName, name);
        }
        if (StringUtils.isNotEmpty(lineNumber)) {
            wrapper.like(XfTransportLine::getLineNumber, lineNumber);
        }
        if (transportLineTypeId != null) {
            wrapper.eq(XfTransportLine::getTransportLineTypeId, transportLineTypeId);
        }
        wrapper.eq(XfTransportLine::getStatus, Constant.DATA_DEFAULT_STATUS);
        wrapper.orderBy(true, false, XfTransportLine::getId);
        return baseMapper.selectPage(iPage, wrapper);
    }

    @Override
    public List<XfTransportLine> findAll(List<Long> ids, Long orgId, List<Long> orgIds) {
        LambdaQueryWrapper<XfTransportLine> wrapper = new LambdaQueryWrapper<>();
        if (ids != null && !ids.isEmpty()) {
            wrapper.in(XfTransportLine::getId, ids);
        }
        if (orgId != null) {
            wrapper.eq(XfTransportLine::getOrgId, orgId);
        }
        if (orgIds != null && !orgIds.isEmpty()) {
            wrapper.in(XfTransportLine::getOrgId, orgIds);
        }
        wrapper.eq(XfTransportLine::getStatus, Constant.DATA_DEFAULT_STATUS);
        wrapper.orderBy(true, false, XfTransportLine::getId);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public void disable(Long id) {
        // 仅更新 status 列：业务字段配了 FieldStrategy.ALWAYS，用实体 updateById 会把未赋值字段一并置 null
        lambdaUpdate().set(XfTransportLine::getStatus, Constant.DATA_DISABLE_STATUS)
                .eq(XfTransportLine::getId, id).update();
    }
}
