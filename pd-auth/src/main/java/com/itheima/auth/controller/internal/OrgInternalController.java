package com.itheima.auth.controller.internal;

import com.itheima.auth.service.internal.OrgQueryService;
import com.itheima.pinda.DTO.OrgDTO;
import com.itheima.pinda.DTO.OrgTreeDTO;
import com.itheima.pinda.common.utils.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 组织内部端点：仅供微服务经服务发现调用，路径与 OrgFeign 对齐。
 */
@RestController
@RequestMapping("/internal/org")
@RequiredArgsConstructor
public class OrgInternalController {

    private final OrgQueryService orgQueryService;

    @GetMapping("/{id}")
    public OrgDTO get(@PathVariable("id") Long id) {
        return orgQueryService.get(id);
    }

    /** 按组织ID反查租户ID（绕过租户隔离），供系统态定时任务使用 */
    @GetMapping("/tenant/{orgId}")
    public Long getTenantIdByOrgId(@PathVariable("orgId") Long orgId) {
        return orgQueryService.getTenantIdByOrgId(orgId);
    }

    @GetMapping("/list")
    public List<OrgDTO> list(@RequestParam(value = "orgType", required = false) Integer orgType,
                             @RequestParam(value = "ids", required = false) List<Long> ids,
                             @RequestParam(value = "countyId", required = false) Long countyId,
                             @RequestParam(value = "parentId", required = false) Long parentId,
                             @RequestParam(value = "parentIds", required = false) List<Long> parentIds) {
        return orgQueryService.list(orgType, ids, countyId, parentId, parentIds);
    }

    @GetMapping("/tree")
    public List<OrgTreeDTO> tree(@RequestParam(value = "parentId", required = false) String parentId,
                                 @RequestParam(value = "flag", required = false) Boolean flag) {
        return orgQueryService.tree(parentId, flag);
    }

    @GetMapping("/page-like")
    public PageResponse<OrgDTO> pageLike(@RequestParam(value = "pageSize") Integer pageSize,
                                         @RequestParam(value = "page") Integer page,
                                         @RequestParam(value = "keyword", required = false) String keyword,
                                         @RequestParam(value = "cityId", required = false) Long cityId,
                                         @RequestParam(value = "latitude", required = false) String latitude,
                                         @RequestParam(value = "longitude", required = false) String longitude) {
        return orgQueryService.pageLike(pageSize, page, keyword, cityId, latitude, longitude);
    }

    @GetMapping("/list-by-county")
    public List<OrgDTO> listByCountyIds(@RequestParam(value = "orgType") Integer orgType,
                                        @RequestParam(value = "countyIds") List<Long> countyIds) {
        return orgQueryService.listByCountyIds(orgType, countyIds);
    }
}
