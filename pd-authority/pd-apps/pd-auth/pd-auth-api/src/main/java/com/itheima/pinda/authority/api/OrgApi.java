package com.itheima.pinda.authority.api;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.pinda.authority.api.hystrix.OrgApiFallback;
import com.itheima.pinda.authority.dto.core.OrgTreeDTO;
import com.itheima.pinda.authority.entity.core.Org;
import com.itheima.pinda.base.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 组织
 */
@FeignClient(name = "${pinda.feign.authority-server:pd-auth-server}", fallback = OrgApiFallback.class, path = "/org")
public interface OrgApi {
    /**
     * 查询组织
     *
     * @param id 主键id
     * @return 查询结果
     */
    @GetMapping("/{id}")
    R<Org> get(@PathVariable Long id);

    /**
     * 根据条件查询组织列表
     *
     * @param orgType  组织类型
     * @param ids      组织id列表
     * @param countyId 区县id
     * @param pid      父级id
     * @param pids     父级id列表
     * @return
     */
    @GetMapping
    R<List<Org>> list(@RequestParam(name = "orgType", required = false) Integer orgType,
                      @RequestParam(name = "ids", required = false) List<Long> ids,
                      @RequestParam(name = "countyId", required = false) Long countyId,
                      @RequestParam(name = "pid", required = false) Long pid,
                      @RequestParam(name = "pids", required = false) List<Long> pids);

    /**
     * 查询系统所有的组织树
     */
    @GetMapping("/tree")
    R<List<OrgTreeDTO>> tree(@RequestParam(value = "name", required = false) String name,
                             @RequestParam(value = "status", required = false) Boolean status);


    @GetMapping("/pageLike")
    R<Page> pageLike(@RequestParam(value = "size", required = false) Integer size,
                     @RequestParam(value = "current", required = false) Integer current,
                     @RequestParam(value = "keyword", required = false) String keyword,
                     @RequestParam(value = "cityId", required = false) Long cityId,
                     @RequestParam(value = "latitude", required = false) String latitude,
                     @RequestParam(value = "longitude", required = false) String longitude);

    /**
     * 根据条件查询组织列表
     *
     * @param orgType   组织类型
     * @param countyIds 区县id 集合
     * @return
     */
    @GetMapping("/listByCountyIds")
    R<List<Org>> listByCountyIds(@RequestParam(name = "orgType", required = false) Integer orgType,
                                 @RequestParam(name = "countyIds", required = false) List<Long> countyIds);
}
