package com.itheima.pinda.feign.transportline;

import com.itheima.pinda.common.utils.PageResponse;
import com.itheima.pinda.common.utils.Result;
import com.itheima.pinda.DTO.transportline.TransportLineTypeDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Hidden;

import java.util.List;

@FeignClient(name = "pd-base")
// 2026-10-10：Spring Cloud 2025 OpenFeign 禁止接口级 @RequestMapping，前缀下沉到各方法路径（URL 不变）
@Hidden
public interface TransportLineTypeFeign {
    /**
     * 添加线路类型
     *
     * @param dto 线路类型信息
     * @return 线路类型信息
     */
    @PostMapping("base/transportLine/type")
    TransportLineTypeDto saveTransportLineType(@RequestBody TransportLineTypeDto dto);

    /**
     * 根据id获取线路类型详情
     *
     * @param id 线路类型id
     * @return 线路类型详情
     */
    @GetMapping("base/transportLine/type/{id}")
    TransportLineTypeDto fineById(@PathVariable(name = "id") String id);

    /**
     * 获取线路类型分页数据
     *
     * @param page       页码
     * @param pageSize   页尺寸
     * @param typeNumber 类型编号
     * @param name       类型名称
     * @param agencyType 机构类型
     * @param status     状态：1=启用 0=停用，不传=查全部
     * @return 线路类型分页数据
     */
    @GetMapping("base/transportLine/type/page")
    PageResponse<TransportLineTypeDto> findByPage(@RequestParam(name = "page") Integer page,
                                                  @RequestParam(name = "pageSize") Integer pageSize,
                                                  @RequestParam(name = "typeNumber", required = false) String typeNumber,
                                                  @RequestParam(name = "name", required = false) String name,
                                                  @RequestParam(name = "agencyType", required = false) Integer agencyType,
                                                  @RequestParam(name = "status", required = false) Integer status);

    /**
     * 获取线路类型列表
     *
     * @param ids 线路类型id列表
     * @return 线路类型列表
     */
    @GetMapping("base/transportLine/type")
    List<TransportLineTypeDto> findAll(@RequestParam(name = "ids", required = false) List<String> ids);

    /**
     * 更新线路类型信息
     *
     * @param id  线路类型id
     * @param dto 线路类型信息
     * @return 线路类型信息
     */
    @PutMapping("base/transportLine/type/{id}")
    TransportLineTypeDto update(@PathVariable(name = "id") String id, @RequestBody TransportLineTypeDto dto);

    /**
     * 删除线路类型
     *
     * @param id 线路类型id
     * @return 返回信息
     */
    @PutMapping("base/transportLine/type/{id}/disable")
    Result disable(@PathVariable(name = "id") String id);

    /**
     * 启用线路类型
     *
     * @param id 线路类型id
     * @return 返回信息
     */
    @PutMapping("base/transportLine/type/{id}/enable")
    Result enable(@PathVariable(name = "id") String id);
}
