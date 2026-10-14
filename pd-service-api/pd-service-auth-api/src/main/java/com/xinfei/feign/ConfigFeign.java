package com.xinfei.feign;

import com.xinfei.DTO.SysConfigDTO;
import com.xinfei.common.utils.PageResponse;
import com.xinfei.common.utils.Result;
import com.xinfei.feign.fallback.ConfigFeignFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 参数配置内部 Feign（CFG-01），对接 pd-auth /internal/config。
 * 不要在接口上加类级 @RequestMapping，路径前缀下沉到方法，避免被本地 MVC 注册冲突。
 */
@FeignClient(value = "pd-auth", fallback = ConfigFeignFallback.class)
public interface ConfigFeign {

    /**
     * 分页查询平台全局参数。
     */
    @GetMapping("/internal/config/page")
    PageResponse<SysConfigDTO> page(@RequestParam("page") Integer page,
                                    @RequestParam("pageSize") Integer pageSize,
                                    @RequestParam(value = "keywords", required = false) String keywords,
                                    @RequestParam(value = "status", required = false) Integer status);

    /**
     * 参数详情（敏感值脱敏、不回显明文）。
     */
    @GetMapping("/internal/config/{id}")
    SysConfigDTO detail(@PathVariable("id") Long id);

    /**
     * 新增参数。
     */
    @PostMapping("/internal/config")
    Result save(@RequestBody SysConfigDTO dto);

    /**
     * 修改参数；configValue 留空表示不修改原值。
     */
    @PutMapping("/internal/config/{id}")
    Result update(@PathVariable("id") Long id, @RequestBody SysConfigDTO dto);

    /**
     * 删除参数（物理删除）。
     */
    @DeleteMapping("/internal/config/{id}")
    Result remove(@PathVariable("id") Long id);
}
