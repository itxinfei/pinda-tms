package com.itheima.pinda.feign.driver;

import com.itheima.pinda.DTO.AppDriverQueryDTO;
import com.itheima.pinda.DTO.DriverJobDTO;
import com.itheima.pinda.common.utils.PageResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import io.swagger.v3.oas.annotations.Hidden;

@FeignClient(name = "pd-aggregation")
@RequestMapping("appDriver")
@Hidden
public interface AppDriverFeign {
    /**
     * 分页查询司机任务
     *
     * @param dto
     * @return
     */
    @PostMapping("page")
    PageResponse<DriverJobDTO> findByPage(@RequestBody AppDriverQueryDTO dto);

}
