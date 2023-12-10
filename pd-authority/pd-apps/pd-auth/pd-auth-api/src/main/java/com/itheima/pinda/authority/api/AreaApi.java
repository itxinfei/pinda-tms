package com.itheima.pinda.authority.api;

import com.itheima.pinda.authority.entity.common.Area;
import com.itheima.pinda.base.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 角色API
 */
@FeignClient(name = "${pinda.feign.authority-server:pd-auth-server}", path = "/area")
public interface AreaApi {
    /**
     * 根据id获取行政区域详情
     *
     * @param id 行政区域id
     * @return 行政区域id
     */
    @GetMapping("/{id}")
    R<Area> get(@PathVariable Long id);

    /**
     * 根据区域编码获取区域
     *
     * @param code
     * @return
     */
    @GetMapping("/code/{code}")
    public R<Area> getByCode(@PathVariable String code);

    /**
     * 获取行政区域信息列表
     *
     * @param parentId 父级id
     * @return 信息列表
     */
    @GetMapping
    R<List<Area>> findAll(@RequestParam(value = "parentId", required = false) Long parentId, @RequestParam(value = "ids", required = false) List<Long> ids);
}
