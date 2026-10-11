package com.itheima.pinda.feign.truck;

import com.itheima.pinda.DTO.truck.TruckLicenseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Hidden;

@FeignClient(name = "pd-base")
// 2026-10-10：Spring Cloud 2025 OpenFeign 禁止接口级 @RequestMapping，前缀下沉到方法路径（URL 不变）
@Hidden
public interface TruckLicenseFeign {
    /**
     * 保存车辆行驶证信息
     *
     * @param dto 车辆行驶证信息
     * @return 车辆行驶证信息
     */
    @PostMapping("base/truck/license")
    TruckLicenseDto saveTruckLicense(@RequestBody TruckLicenseDto dto);

    /**
     * 根据id获取车辆行驶证详情
     *
     * @param id 车辆行驶证id
     * @return 车辆行驶证信息
     */
    @GetMapping("base/truck/license/{id}")
    TruckLicenseDto fineById(@PathVariable(name = "id") String id);
}
