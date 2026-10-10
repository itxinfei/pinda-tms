package com.itheima.pinda.feign;

import com.itheima.pinda.DTO.DriverJobDTO;
import com.itheima.pinda.common.utils.PageResponse;
import com.itheima.pinda.feign.fallback.DriverJobFeignFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 不要在接口上加类级 @RequestMapping（会与 fallback 同时被 MVC 注册，报 Ambiguous mapping）。
@FeignClient(value = "pd-work", fallback = DriverJobFeignFallback.class)
public interface DriverJobFeign {
    /**
     * 新增司机作业单
     *
     * @param dto 司机作业单信息
     * @return 司机作业单信息
     */
    @PostMapping("/driver-job")
    DriverJobDTO save(@RequestBody DriverJobDTO dto);

    /**
     * 修改司机作业单信息
     *
     * @param id  司机作业单id
     * @param dto 司机作业单信息
     * @return 司机作业单信息
     */
    @PutMapping("/driver-job/{id}")
    DriverJobDTO updateById(@PathVariable(name = "id") Long id, @RequestBody DriverJobDTO dto);

    /**
     * 获取司机作业单分页数据
     *
     * @param dto 查询参数
     * @return 司机作业单分页数据
     */
    @PostMapping("/driver-job/page")
    PageResponse<DriverJobDTO> findByPage(@RequestBody DriverJobDTO dto);

    /**
     * 根据id获取司机作业单信息
     *
     * @param id 司机作业单id
     * @return 司机作业单信息
     */
    @GetMapping("/driver-job/{id}")
    DriverJobDTO findById(@PathVariable(name = "id") Long id);

    /**
     * 根据参数查询全部信息
     *
     * @param dto 查询条件
     * @return 司机作业单集合
     */
    @PostMapping("/driver-job/findAll")
    List<DriverJobDTO> findAll(@RequestBody DriverJobDTO dto);
}
