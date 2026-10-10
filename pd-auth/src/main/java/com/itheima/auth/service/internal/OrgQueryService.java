package com.itheima.auth.service.internal;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.auth.entity.AuthUser;
import com.itheima.auth.entity.CoreOrg;
import com.itheima.auth.mapper.AuthUserMapper;
import com.itheima.auth.mapper.CoreOrgMapper;
import com.itheima.pinda.DTO.OrgDTO;
import com.itheima.pinda.DTO.OrgTreeDTO;
import com.itheima.pinda.common.utils.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 组织内部查询：供 /internal/org 端点调用，返回裸 DTO。
 * manager（负责人姓名）由 managerId 关联用户表批量装配。
 */
@Service
@RequiredArgsConstructor
public class OrgQueryService {

    private final CoreOrgMapper orgMapper;
    private final AuthUserMapper userMapper;

    /**
     * 按ID查询
     */
    public OrgDTO get(Long id) {
        CoreOrg org = orgMapper.selectById(id);
        if (org == null) {
            return null;
        }
        return toDTO(org, loadManagerNames(List.of(org)));
    }

    /**
     * 按组织ID反查租户ID（绕过租户插件），供系统态定时任务解析租户。
     */
    public Long getTenantIdByOrgId(Long orgId) {
        if (orgId == null) {
            return null;
        }
        return orgMapper.selectTenantIdByOrgId(orgId);
    }

    /**
     * 多条件查询
     */
    public List<OrgDTO> list(Integer orgType, List<Long> ids, Long countyId,
                             Long parentId, List<Long> parentIds) {
        List<CoreOrg> orgs = orgMapper.selectList(Wrappers.<CoreOrg>lambdaQuery()
                .eq(orgType != null, CoreOrg::getOrgType, orgType)
                .in(ids != null && !ids.isEmpty(), CoreOrg::getId, ids)
                .eq(countyId != null, CoreOrg::getCountyId, countyId)
                .eq(parentId != null, CoreOrg::getParentId, parentId)
                .in(parentIds != null && !parentIds.isEmpty(), CoreOrg::getParentId, parentIds)
                .orderByAsc(CoreOrg::getSortValue)
                .orderByAsc(CoreOrg::getId));
        Map<Long, String> managerNames = loadManagerNames(orgs);
        return orgs.stream().map(o -> toDTO(o, managerNames)).collect(Collectors.toList());
    }

    /**
     * 组织树。parentId 为 null / 空串均表示从根开始（兼容不同调用方传参）。
     */
    public List<OrgTreeDTO> tree(String parentId, Boolean flag) {
        Long rootId = StringUtils.hasText(parentId) ? Long.valueOf(parentId.trim()) : 0L;
        List<CoreOrg> all = orgMapper.selectList(Wrappers.<CoreOrg>lambdaQuery()
                .orderByAsc(CoreOrg::getSortValue)
                .orderByAsc(CoreOrg::getId));
        Map<Long, List<CoreOrg>> byParent = all.stream()
                .collect(Collectors.groupingBy(o -> o.getParentId() == null ? 0L : o.getParentId()));
        return buildTree(byParent.getOrDefault(rootId, List.of()), byParent);
    }

    /**
     * 分页模糊查询附近组织（按距离升序）
     */
    public PageResponse<OrgDTO> pageLike(Integer pageSize, Integer page, String keyword,
                                         Long cityId, String latitude, String longitude) {
        long pageNo = page == null ? 1 : page;
        long size = pageSize == null ? 10 : pageSize;
        IPage<CoreOrg> result = orgMapper.pageLike(new Page<>(pageNo, size),
                cityId, keyword, latitude, longitude);
        List<OrgDTO> items = result.getRecords().stream()
                .map(this::toSimpleDTO).collect(Collectors.toList());
        return PageResponse.<OrgDTO>builder()
                .counts(result.getTotal())
                .page((int) result.getCurrent())
                .pagesize((int) result.getSize())
                .pages(result.getPages())
                .items(items)
                .build();
    }

    /**
     * 按区行政区划ID集合查询某类组织
     */
    public List<OrgDTO> listByCountyIds(Integer orgType, List<Long> countyIds) {
        if (countyIds == null || countyIds.isEmpty()) {
            return List.of();
        }
        List<CoreOrg> orgs = orgMapper.selectList(Wrappers.<CoreOrg>lambdaQuery()
                .eq(orgType != null, CoreOrg::getOrgType, orgType)
                .in(CoreOrg::getCountyId, countyIds));
        Map<Long, String> managerNames = loadManagerNames(orgs);
        return orgs.stream().map(o -> toDTO(o, managerNames)).collect(Collectors.toList());
    }

    // ---------------- 私有方法 ----------------

    /**
     * 递归组装组织树 DTO
     */
    private List<OrgTreeDTO> buildTree(List<CoreOrg> nodes, Map<Long, List<CoreOrg>> byParent) {
        List<OrgTreeDTO> tree = new ArrayList<>();
        for (CoreOrg node : nodes) {
            OrgTreeDTO dto = new OrgTreeDTO();
            mapOrgFields(node, dto);
            List<CoreOrg> children = byParent.get(node.getId());
            if (children != null && !children.isEmpty()) {
                dto.setChildren(buildTree(children, byParent));
            }
            tree.add(dto);
        }
        return tree;
    }

    /**
     * 批量加载负责人姓名，返回 managerId -> name
     */
    private Map<Long, String> loadManagerNames(List<CoreOrg> orgs) {
        Set<Long> managerIds = orgs.stream()
                .map(CoreOrg::getManagerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (managerIds.isEmpty()) {
            return Map.of();
        }
        List<AuthUser> managers = userMapper.selectList(Wrappers.<AuthUser>lambdaQuery()
                .in(AuthUser::getId, managerIds));
        return managers.stream()
                .collect(Collectors.toMap(AuthUser::getId, AuthUser::getName, (a, b) -> a));
    }

    private OrgDTO toDTO(CoreOrg org, Map<Long, String> managerNames) {
        OrgDTO dto = toSimpleDTO(org);
        if (org.getManagerId() != null) {
            dto.setManager(managerNames.get(org.getManagerId()));
        }
        return dto;
    }

    private OrgDTO toSimpleDTO(CoreOrg org) {
        OrgDTO dto = new OrgDTO();
        mapOrgFields(org, dto);
        return dto;
    }

    /**
     * 把实体字段写入组织 DTO。同名同类型字段走 copyProperties；
     * 类型不一致字段（status、经纬度、省市区ID）手工转换——这些字段 copyProperties 会跳过。
     */
    private void mapOrgFields(CoreOrg org, OrgDTO dto) {
        BeanUtils.copyProperties(org, dto);
        dto.setStatus(org.getStatus() != null && org.getStatus() == 1);
        dto.setLongitude(org.getLongitude() == null ? null : org.getLongitude().toPlainString());
        dto.setLatitude(org.getLatitude() == null ? null : org.getLatitude().toPlainString());
        dto.setProvinceId(org.getProvinceId() == null ? null : org.getProvinceId().longValue());
        dto.setCityId(org.getCityId() == null ? null : org.getCityId().longValue());
        dto.setCountyId(org.getCountyId() == null ? null : org.getCountyId().longValue());
    }
}
