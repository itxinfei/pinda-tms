package com.itheima.pinda.feign;

import com.itheima.pinda.DTO.OrgDTO;
import com.itheima.pinda.DTO.OrgTreeDTO;
import com.itheima.pinda.common.utils.PageResponse;
import com.itheima.pinda.feign.fallback.OrgFeignFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

// 注意：不要在接口上加类级 @RequestMapping，否则 Feign 代理与 fallback @Component 会注册相同路径，
// 启动报 Ambiguous mapping。路径前缀下沉到每个方法。
@FeignClient(value = "pd-auth", fallback = OrgFeignFallback.class)
public interface OrgFeign {

    /**
     * 按ID查询组织
     */
    @GetMapping("/internal/org/{id}")
    OrgDTO get(@PathVariable("id") Long id);

    /**
     * 按组织ID反查租户ID（绕过租户隔离）。供系统态定时任务在无租户上下文时解析租户。
     */
    @GetMapping("/internal/org/tenant/{orgId}")
    Long getTenantIdByOrgId(@PathVariable("orgId") Long orgId);

    /**
     * 多条件查询组织列表
     *
     * @param orgType   组织类型
     * @param ids       组织ID集合
     * @param countyId  区行政区划ID
     * @param parentId  父组织ID
     * @param parentIds 父组织ID集合
     */
    @GetMapping("/internal/org/list")
    List<OrgDTO> list(@RequestParam(value = "orgType", required = false) Integer orgType,
                      @RequestParam(value = "ids", required = false) List<Long> ids,
                      @RequestParam(value = "countyId", required = false) Long countyId,
                      @RequestParam(value = "parentId", required = false) Long parentId,
                      @RequestParam(value = "parentIds", required = false) List<Long> parentIds);

    /**
     * 查询组织树。parentId 需同时容忍 null 与空串（兼容不同调用方传参）。
     */
    @GetMapping("/internal/org/tree")
    List<OrgTreeDTO> tree(@RequestParam(value = "parentId", required = false) String parentId,
                          @RequestParam(value = "flag", required = false) Boolean flag);

    /**
     * 分页模糊查询附近组织（供客户端寄件选网点）
     */
    @GetMapping("/internal/org/page-like")
    PageResponse<OrgDTO> pageLike(@RequestParam("pageSize") Integer pageSize,
                                  @RequestParam("page") Integer page,
                                  @RequestParam(value = "keyword", required = false) String keyword,
                                  @RequestParam(value = "cityId", required = false) Long cityId,
                                  @RequestParam(value = "latitude", required = false) String latitude,
                                  @RequestParam(value = "longitude", required = false) String longitude);

    /**
     * 按区行政区划ID集合查询某类组织
     */
    @GetMapping("/internal/org/list-by-county")
    List<OrgDTO> listByCountyIds(@RequestParam("orgType") Integer orgType,
                                 @RequestParam("countyIds") List<Long> countyIds);
}
