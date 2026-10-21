package com.xinfei.service.transportline.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xinfei.common.utils.Constant;
import com.xinfei.entity.transportline.XfTransportTrips;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinfei.mapper.transportline.XfTransportTripsMapper;
import com.xinfei.service.transportline.IPdTransportTripsService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 车次 服务实现类
 */
@Service
public class XfTransportTripsServiceImpl extends ServiceImpl<XfTransportTripsMapper, XfTransportTrips>
        implements IPdTransportTripsService {

    @Override
    public XfTransportTrips saveTransportTrips(XfTransportTrips xfTransportTrips) {
        baseMapper.insert(xfTransportTrips);
        return xfTransportTrips;
    }

    @Override
    public List<XfTransportTrips> findAll(Long transportLineId, List<Long> ids) {
        LambdaQueryWrapper<XfTransportTrips> wrapper = new LambdaQueryWrapper<>();
        if (transportLineId != null) {
            wrapper.eq(XfTransportTrips::getTransportLineId, transportLineId);
        }
        if (ids != null && !ids.isEmpty()) {
            wrapper.in(XfTransportTrips::getId, ids);
        }
        wrapper.eq(XfTransportTrips::getStatus, Constant.DATA_DEFAULT_STATUS);
        wrapper.orderBy(true, true, XfTransportTrips::getId);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public void disable(Long id) {
        XfTransportTrips trips = new XfTransportTrips();
        trips.setId(id);
        trips.setStatus(Constant.DATA_DISABLE_STATUS);
        baseMapper.updateById(trips);
    }
}
