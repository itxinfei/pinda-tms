package com.xinfei.feign.agency;

import com.xinfei.common.utils.PageResponse;
import com.xinfei.common.utils.Result;
import com.xinfei.DTO.angency.FleetDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Hidden;

import java.util.List;

@FeignClient(name = "pd-base")
// 2026-10-10：Spring Cloud 2025 OpenFeign 禁止接口级 @RequestMapping，
// 原 "sys/agency/fleet" 前缀下沉到各方法路径（URL 不变）
@Hidden
public interface FleetFeign {
    /**
     * 添加车队
     *
     * @param dto 车队信息
     * @return 车队信息
     */
    @PostMapping("sys/agency/fleet")
    FleetDto saveAgencyType(@RequestBody FleetDto dto);

    /**
     * 根据id获取车队详情
     *
     * @param id 车队id
     * @return 车队信息
     */
    @GetMapping("sys/agency/fleet/{id}")
    FleetDto fineById(@PathVariable(name = "id") String id);

    /**
     * 获取车队分页数据
     *
     * @param page        页码
     * @param pageSize    页尺寸
     * @param name        车队名称
     * @param fleetNumber 车队编号
     * @param manager     负责人id
     * @param status      状态：1=启用 0=停用，不传=查全部
     * @return 车队分页数据
     */
    @GetMapping("sys/agency/fleet/page")
    PageResponse<FleetDto> findByPage(@RequestParam(name = "page") Integer page,
                                      @RequestParam(name = "pageSize") Integer pageSize,
                                      @RequestParam(name = "name", required = false) String name,
                                      @RequestParam(name = "fleetNumber", required = false) String fleetNumber,
                                      @RequestParam(name = "manager", required = false) String manager,
                                      @RequestParam(name = "status", required = false) Integer status);

    /**
     * 获取车队列表
     *
     * @param ids 车队Id列表
     * @return 车队列表
     */
    @GetMapping("sys/agency/fleet")
    List<FleetDto> findAll(@RequestParam(value = "ids",required = false) List<String> ids,@RequestParam(value = "agencyId",required = false) String agencyId);

    /**
     * 更新车队信息
     *
     * @param dto 车队信息
     * @return 车队信息
     */
    @PutMapping("sys/agency/fleet/{id}")
    FleetDto update(@PathVariable(name = "id") String id, @RequestBody FleetDto dto);

    /**
     * 删除车队
     *
     * @param id 车队id
     * @return 返回信息
     */
    @PutMapping("sys/agency/fleet/{id}/disable")
    Result disable(@PathVariable(name = "id") String id);

    /**
     * 启用车队
     *
     * @param id 车队id
     * @return 返回信息
     */
    @PutMapping("sys/agency/fleet/{id}/enable")
    Result enable(@PathVariable(name = "id") String id);
}
