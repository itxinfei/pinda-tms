package com.itheima.pinda.feign;

import com.itheima.pinda.DTO.AreaDTO;
import com.itheima.pinda.feign.fallback.AreaFeignFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 行政区划内部 Feign。
 */
@FeignClient(value = "pd-auth", fallback = AreaFeignFallback.class)
public interface AreaFeign {

    /**
     * 按ID查询行政区划
     */
    @GetMapping("/internal/area/{id}")
    AreaDTO get(@PathVariable("id") Long id);

    /**
     * 按父级ID或ID集合查询行政区划
     */
    @GetMapping("/internal/area/list")
    List<AreaDTO> findAll(@RequestParam(value = "parentId", required = false) Long parentId,
                          @RequestParam(value = "ids", required = false) List<Long> ids);

    /**
     * 按区划编码查询（调用方多传入 adcode 补零后的编码）
     */
    @GetMapping("/internal/area/code/{code}")
    AreaDTO getByCode(@PathVariable("code") String code);
}
