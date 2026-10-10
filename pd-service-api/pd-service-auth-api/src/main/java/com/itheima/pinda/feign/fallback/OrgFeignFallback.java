package com.itheima.pinda.feign.fallback;

import com.itheima.pinda.DTO.OrgDTO;
import com.itheima.pinda.DTO.OrgTreeDTO;
import com.itheima.pinda.common.utils.PageResponse;
import com.itheima.pinda.feign.OrgFeign;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 组织 Feign 熔断降级
 */
@Slf4j
@Component
public class OrgFeignFallback implements OrgFeign {

    @Override
    public OrgDTO get(Long id) {
        log.warn("远程调用 pd-auth 失败: OrgFeign.get({}), 返回null", id);
        return null;
    }

    @Override
    public Long getTenantIdByOrgId(Long orgId) {
        log.warn("远程调用 pd-auth 失败: OrgFeign.getTenantIdByOrgId({}), 返回null", orgId);
        return null;
    }

    @Override
    public List<OrgDTO> list(Integer orgType, List<Long> ids, Long countyId, Long parentId, List<Long> parentIds) {
        log.warn("远程调用 pd-auth 失败: OrgFeign.list, 返回空列表");
        return Collections.emptyList();
    }

    @Override
    public List<OrgTreeDTO> tree(String parentId, Boolean flag) {
        log.warn("远程调用 pd-auth 失败: OrgFeign.tree, 返回空列表");
        return Collections.emptyList();
    }

    @Override
    public PageResponse<OrgDTO> pageLike(Integer pageSize, Integer page, String keyword,
                                         Long cityId, String latitude, String longitude) {
        log.warn("远程调用 pd-auth 失败: OrgFeign.pageLike, 返回空分页");
        return emptyPage();
    }

    @Override
    public List<OrgDTO> listByCountyIds(Integer orgType, List<Long> countyIds) {
        log.warn("远程调用 pd-auth 失败: OrgFeign.listByCountyIds, 返回空列表");
        return Collections.emptyList();
    }

    private PageResponse<OrgDTO> emptyPage() {
        return PageResponse.<OrgDTO>builder()
                .counts(0L)
                .pagesize(0)
                .pages(0L)
                .page(0)
                .items(Collections.emptyList())
                .build();
    }
}
