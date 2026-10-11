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
// 2026-10-10：Spring Cloud 2025 OpenFeign 禁止接口级 @RequestMapping，前缀下沉到方法路径（URL 不变）
@Hidden
public interface AppDriverFeign {
    /**
     * 分页查询司机任务
     *
     * @param dto
     * @return
     */
    @PostMapping("appDriver/page")
    PageResponse<DriverJobDTO> findByPage(@RequestBody AppDriverQueryDTO dto);

}
