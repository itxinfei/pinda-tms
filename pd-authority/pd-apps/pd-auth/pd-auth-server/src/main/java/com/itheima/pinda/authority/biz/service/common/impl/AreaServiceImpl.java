package com.itheima.pinda.authority.biz.service.common.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.pinda.authority.biz.dao.common.AreaMapper;
import com.itheima.pinda.authority.biz.service.common.AreaService;
import com.itheima.pinda.authority.entity.common.Area;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 业务实现类
 * 行政区域
 * </p>
 */
@Slf4j
@Service
public class AreaServiceImpl extends ServiceImpl<AreaMapper, Area> implements AreaService {
    @Override
    public List<Area> findAll(Long parentId, List<Long> ids) {
        LambdaQueryWrapper<Area> queryWrapper = new LambdaQueryWrapper<>();
        boolean isLimit = true;
        if (parentId != null) {
            queryWrapper.eq(Area::getParentId, parentId);
            isLimit = false;
        }
        if (ids != null && ids.size() > 0) {
            queryWrapper.in(Area::getId, ids);
            isLimit = false;
        }
        if (isLimit) {
            queryWrapper.last("limit 100");
        }
        queryWrapper.orderBy(true, true, Area::getId);
        return baseMapper.selectList(queryWrapper);
    }

    @Override
    public Area getByCode(String code) {
        return baseMapper.selectOne(new QueryWrapper<Area>().eq("area_code", code).last(" limit 1"));
    }
}
