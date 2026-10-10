package com.itheima.auth.service.internal;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.itheima.auth.entity.Area;
import com.itheima.auth.mapper.AreaMapper;
import com.itheima.pinda.DTO.AreaDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 行政区划内部查询：供 /internal/area 端点调用，返回裸 DTO。
 */
@Service
@RequiredArgsConstructor
public class AreaQueryService {

    private final AreaMapper areaMapper;

    /**
     * 按ID查询
     */
    public AreaDTO get(Long id) {
        Area area = areaMapper.selectById(id);
        return area == null ? null : toDTO(area);
    }

    /**
     * 按父级ID或ID集合查询
     */
    public List<AreaDTO> findAll(Long parentId, List<Long> ids) {
        List<Area> areas = areaMapper.selectList(Wrappers.<Area>lambdaQuery()
                .eq(parentId != null, Area::getParentId, parentId)
                .in(ids != null && !ids.isEmpty(), Area::getId, ids)
                .orderByAsc(Area::getId));
        return areas.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * 按行政区划代码查询
     */
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
