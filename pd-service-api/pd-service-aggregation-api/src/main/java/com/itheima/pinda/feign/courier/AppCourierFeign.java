package com.itheima.pinda.feign.courier;

import com.itheima.pinda.DTO.AppCourierQueryDTO;
import com.itheima.pinda.DTO.TaskPickupDispatchDTO;
import com.itheima.pinda.common.utils.PageResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import io.swagger.v3.oas.annotations.Hidden;

@FeignClient(name = "pd-aggregation")
// 2026-10-10：Spring Cloud 2025 OpenFeign 禁止接口级 @RequestMapping，前缀下沉到方法路径（URL 不变）
@Hidden
public interface AppCourierFeign {
    /**
     * 分页查询快递员任务
     *
     * @param dto
     * @return
     */
    @PostMapping("appCourier/page")
    PageResponse<TaskPickupDispatchDTO> findByPage(@RequestBody AppCourierQueryDTO dto);

}
