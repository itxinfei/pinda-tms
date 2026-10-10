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
@RequestMapping("appCourier")
@Hidden
public interface AppCourierFeign {
    /**
     * 分页查询快递员任务
     *
     * @param dto
     * @return
     */
    @PostMapping("page")
    PageResponse<TaskPickupDispatchDTO> findByPage(@RequestBody AppCourierQueryDTO dto);

}
