package com.itheima.auth.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.auth.common.BizException;
import com.itheima.auth.entity.CoreStation;
import com.itheima.auth.mapper.CoreStationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 岗位管理：分页查询、新增、修改、删除。租户隔离由 TenantLineInnerInterceptor 自动处理。
 */
@Service
@RequiredArgsConstructor
public class StationService {

    private final CoreStationMapper stationMapper;

    /**
     * 分页查询，可按岗位名称模糊、所属组织过滤
     */
    public IPage<CoreStation> page(long pageNo, long size, String name, Long orgId) {
        return stationMapper.selectPage(new Page<>(pageNo, size), Wrappers.<CoreStation>lambdaQuery()
                .like(StringUtils.hasText(name), CoreStation::getName, name)
                .eq(orgId != null, CoreStation::getOrgId, orgId)
                .orderByDesc(CoreStation::getId));
    }

    /**
     * 岗位详情
     */
    public CoreStation detail(Long id) {
        CoreStation station = stationMapper.selectById(id);
        if (station == null) {
            throw new BizException("岗位不存在");
        }
        return station;
    }

    /**
     * 新增岗位，返回新ID
     */
    public Long save(CoreStation station) {
        station.setId(null);
        station.setCreateUser(StpUtil.getLoginIdAsLong());
        station.setCreateTime(LocalDateTime.now());
        stationMapper.insert(station);
        return station.getId();
    }

    /**
     * 修改岗位
     */
    public void update(CoreStation station) {
        if (station.getId() == null) {
            throw new BizException("缺少岗位ID");
        }
        if (stationMapper.selectById(station.getId()) == null) {
            throw new BizException("岗位不存在");
        }
        // tenantId 不在此更新
        station.setUpdateUser(StpUtil.getLoginIdAsLong());
        station.setUpdateTime(LocalDateTime.now());
        stationMapper.updateById(station);
    }

    /**
     * 删除岗位
     */
    public void remove(Long id) {
        stationMapper.deleteById(id);
    }
}
