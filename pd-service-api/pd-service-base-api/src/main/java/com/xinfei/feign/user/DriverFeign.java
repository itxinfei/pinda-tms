package com.xinfei.feign.user;

import com.xinfei.DTO.user.TruckDriverDto;
import com.xinfei.DTO.user.TruckDriverLicenseDto;
import com.xinfei.common.utils.PageResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Hidden;

import java.util.List;

@FeignClient(name = "pd-base")
// 2026-10-10：Spring Cloud 2025 OpenFeign 禁止接口级 @RequestMapping，前缀下沉到各方法路径（URL 不变）
@Hidden
public interface DriverFeign {
    /**
     * 保存司机基本信息
     *
     * @param dto 司机基本信息
     * @return 返回信息
     */
    @PostMapping("sys/driver")
    TruckDriverDto saveDriver(@RequestBody TruckDriverDto dto);

    /**
     * 获取司机基本信息列表
     *
     * @param userIds 司机id列表
     * @param fleetId 车队id
     * @return 司机基本信息列表
     */
    @GetMapping("sys/driver")
    List<TruckDriverDto> findAllDriver(@RequestParam(name = "userIds",required = false) List<String> userIds, @RequestParam(name = "fleetId",required = false) String fleetId);

    /**
     * 获取司机基本信息
     *
     * @param id 司机id
     * @return 司机基本信息
     */
    @GetMapping("sys/driver/{id}")
    TruckDriverDto findOneDriver(@PathVariable(name = "id") String id);

    /**
     * 保存司机驾驶证信息
     *
     * @param dto 司机驾驶证信息
     * @return 返回信息
     */
    @PostMapping("sys/driver/driverLicense")
    TruckDriverLicenseDto saveDriverLicense(@RequestBody TruckDriverLicenseDto dto);

    /**
     * 获取司机驾驶证信息
     *
     * @param id 司机id
     * @return 司机驾驶证信息
     */
    @GetMapping("sys/driver/{id}/driverLicense")
    TruckDriverLicenseDto findOneDriverLicense(@PathVariable(name = "id") String id);

    /**
     * 统计司机数量
     *
     * @param fleetId 车队id
     * @return 司机数量
     */
    @GetMapping("sys/driver/count")
    Integer count(@RequestParam(name = "fleetId", required = false) String fleetId);

    /**
     * 获取司机分页数据
     *
     * @param page     页码
     * @param pageSize 页尺寸
     * @param fleetId  车队id
     * @return 司机分页数据
     */
    @GetMapping("sys/driver/page")
    PageResponse<TruckDriverDto> findByPage(@RequestParam(name = "page") Integer page,
                                            @RequestParam(name = "pageSize") Integer pageSize,
                                            @RequestParam(name = "fleetId", required = false) String fleetId);


    /**
     * 查询全部
     * @param ids
     * @return
     */
    @GetMapping("sys/driver/findAll")
    List<TruckDriverDto> findAll(@RequestParam(name = "ids", required = false) List<String> ids);
}
