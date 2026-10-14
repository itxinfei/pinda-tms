package com.xinfei.feign.common;

import com.xinfei.common.utils.PageResponse;
import com.xinfei.common.utils.Result;
import com.xinfei.DTO.base.GoodsTypeDto;
import com.xinfei.feign.common.fallback.GoodsTypeFeignFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Hidden;

import java.util.List;

@FeignClient(value = "pd-base", fallback = GoodsTypeFeignFallback.class, path = "/base/goodsType")
@Hidden
public interface GoodsTypeFeign {
    /**
     * 添加货物类型
     *
     * @param dto 货物类型信息
     * @return 货物类型信息
     */
    @PostMapping("")
    GoodsTypeDto saveGoodsType(@RequestBody GoodsTypeDto dto);

    /**
     * 根据id获取货物类型详情
     *
     * @param id 货物类型id
     * @return 货物类型信息
     */
    @GetMapping("/{id}")
    GoodsTypeDto fineById(@PathVariable(name = "id") String id);

    /**
     * 获取货物类型列表
     *
     * @return 货物类型列表
     */
    @GetMapping("")
    public List<GoodsTypeDto> findAll(@RequestParam(name = "ids",required = false) List<String> ids);

    @GetMapping("all")
    public List<GoodsTypeDto> findAll();

    /**
     * 获取分页货物类型数据
     *
     * @param page     页码
     * @param pageSize 页尺寸
     * @return 分页货物类型数据
     */
    @GetMapping("/page")
    PageResponse<GoodsTypeDto> findByPage(
            @RequestParam(name = "page") Integer page,
            @RequestParam(name = "pageSize") Integer pageSize,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "truckTypeId", required = false) String truckTypeId,
            @RequestParam(name = "truckTypeName", required = false) String truckTypeName,
            @RequestParam(name = "status", required = false) Integer status);

    /**
     * 更新货物类型信息
     *
     * @param dto 货物类型信息
     * @return 货物类型信息
     */
    @PutMapping("/{id}")
    GoodsTypeDto update(@PathVariable(name = "id") String id, @RequestBody GoodsTypeDto dto);

    /**
     * 删除货物类型
     *
     * @param id 货物类型id
     * @return 返回信息
     */
    @PutMapping("/{id}/disable")
    Result disable(@PathVariable(name = "id") String id);

    /**
     * 启用货物类型
     *
     * @param id 货物类型id
     * @return 返回信息
     */
    @PutMapping("/{id}/enable")
    Result enable(@PathVariable(name = "id") String id);
}
