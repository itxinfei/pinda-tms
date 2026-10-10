package com.itheima.pinda.service.transportline.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.itheima.pinda.common.utils.Constant;
import com.itheima.pinda.entity.transportline.PdTransportTrips;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.pinda.mapper.transportline.PdTransportTripsMapper;
import com.itheima.pinda.service.transportline.IPdTransportTripsService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 车次 服务实现类
 */
@Service
public class PdTransportTripsServiceImpl extends ServiceImpl<PdTransportTripsMapper, PdTransportTrips>
        implements IPdTransportTripsService {

    @Override
    public PdTransportTrips saveTransportTrips(PdTransportTrips pdTransportTrips) {
        baseMapper.insert(pdTransportTrips);
        return pdTransportTrips;
    }

    @Override
    public List<PdTransportTrips> findAll(Long transportLineId, List<Long> ids) {
        LambdaQueryWrapper<PdTransportTrips> wrapper = new LambdaQueryWrapper<>();
        if (transportLineId != null) {
            wrapper.eq(PdTransportTrips::getTransportLineId, transportLineId);
        }
        if (ids != null && !ids.isEmpty()) {
            wrapper.in(PdTransportTrips::getId, ids);
        }
        wrapper.eq(PdTransportTrips::getStatus, Constant.DATA_DEFAULT_STATUS);
        wrapper.orderBy(true, true, PdTransportTrips::getId);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public void disable(Long id) {
        PdTransportTrips trips = new PdTransportTrips();
        trips.setId(id);
        trips.setStatus(Constant.DATA_DISABLE_STATUS);
        baseMapper.updateById(trips);
    }
}
