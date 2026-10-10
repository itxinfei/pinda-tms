package com.itheima.auth.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.itheima.auth.common.BizException;
import com.itheima.auth.entity.Area;
import com.itheima.auth.mapper.AreaMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 行政区划查询。全国数据量大，采用按父级代码懒加载（供省市区级联选择器逐级拉取）。
 * 全局共享数据，不走租户隔离。
 */
@Service
@RequiredArgsConstructor
public class AreaService {

    private final AreaMapper areaMapper;

    /**
     * 查询某一级的直接下级。parentId 为空或 0 返回省级列表。
     */
    public List<Area> children(Long parentId) {
        Long pid = parentId == null ? 0L : parentId;
        return areaMapper.selectList(Wrappers.<Area>lambdaQuery()
                .eq(Area::getParentId, pid)
                .orderByAsc(Area::getId));
    }

    /**
     * 按名称模糊查询（用于地址联想等场景）
     */
    public List<Area> search(String name) {
        if (!StringUtils.hasText(name)) {
            return List.of();
        }
        return areaMapper.selectList(Wrappers.<Area>lambdaQuery()
                .like(Area::getName, name)
                .last("limit 20"));
    }

    /**
     * 行政区划详情
     */
    public Area detail(Long id) {
        Area area = areaMapper.selectById(id);
        if (area == null) {
            throw new BizException("行政区划不存在");
        }
        return area;
    }
}
