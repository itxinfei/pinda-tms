package com.xinfei.auth.service.internal;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xinfei.auth.entity.Area;
import com.xinfei.auth.mapper.AreaMapper;
import com.xinfei.DTO.AreaDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 行政区划内部查询：供 /internal/area 端点调用，返回裸 DTO。
 *
 * <p>区划数据为国家标准种子数据，低频变更、高频读取，故对只读方法加进程内缓存。
 * 因无写入口（区划不在此服务维护），无需 @CacheEvict；若后续引入区划写路径，
 * 应在写方法上补 @CacheEvict 失效下方 area / areaCode / areaChildren 缓存。</p>
 */
@Service
@RequiredArgsConstructor
public class AreaQueryService {

    private final AreaMapper areaMapper;

    /**
     * 按ID查询
     */
    @Cacheable(cacheNames = "area", key = "#id", unless = "#result == null")
    public AreaDTO get(Long id) {
        Area area = areaMapper.selectById(id);
        return area == null ? null : toDTO(area);
    }

    /**
     * 按父级ID或ID集合查询。key 由 Spring 默认 SimpleKey 组合 parentId + ids，多参数组合安全不串。
     */
    @Cacheable(cacheNames = "areaChildren")
    public List<AreaDTO> findAll(Long parentId, List<Long> ids) {
        // D-02 服务端兜底（《12》§8.1）：parentId 与 ids 均空时禁止无条件全表扫描（dict_area 66 万行）
        if (parentId == null && (ids == null || ids.isEmpty())) {
            return Collections.emptyList();
        }
        List<Area> areas = areaMapper.selectList(Wrappers.<Area>lambdaQuery()
                .eq(parentId != null, Area::getParentId, parentId)
                .in(ids != null && !ids.isEmpty(), Area::getId, ids)
                .orderByAsc(Area::getId));
        return areas.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * 按行政区划代码查询
     */
    @Cacheable(cacheNames = "areaCode", key = "#code", unless = "#result == null")
    public AreaDTO getByCode(String code) {
        Area area = areaMapper.selectOne(Wrappers.<Area>lambdaQuery()
                .eq(Area::getAreaCode, code));
        return area == null ? null : toDTO(area);
    }

    private AreaDTO toDTO(Area area) {
        AreaDTO dto = new AreaDTO();
        dto.setId(area.getId().longValue());
        dto.setName(area.getName());
        return dto;
    }
}
