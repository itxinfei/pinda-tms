package com.itheima.pinda.feign.webManager;

import com.itheima.pinda.DTO.DriverJobDTO;
import com.itheima.pinda.DTO.TaskPickupDispatchDTO;
import com.itheima.pinda.DTO.TaskTransportDTO;
import com.itheima.pinda.DTO.TransportOrderDTO;
import com.itheima.pinda.DTO.webManager.DriverJobQueryDTO;
import com.itheima.pinda.DTO.webManager.TaskPickupDispatchQueryDTO;
import com.itheima.pinda.DTO.webManager.TaskTransportQueryDTO;
import com.itheima.pinda.DTO.webManager.TransportOrderQueryDTO;
import com.itheima.pinda.common.utils.PageResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import io.swagger.v3.oas.annotations.Hidden;

@FeignClient(name = "pd-aggregation")
// 2026-10-10：Spring Cloud 2025 OpenFeign 禁止接口级 @RequestMapping，前缀下沉到各方法路径（URL 不变）
@Hidden
public interface WebManagerFeign {
    /**
     * 分页查询司机任务
     *
     * @param dto 查询参数
     * @return 司机任务分页数据
     */
    @PostMapping("webManager/driverJob/page")
    PageResponse<DriverJobDTO> findDriverJobByPage(@RequestBody DriverJobQueryDTO dto);

    /**
     * 分页查询取派件任务
     *
     * @param dto 查询参数
     * @return 取派件任务分页数据
     */
    @PostMapping("webManager/taskPickupDispatchJob/page")
    PageResponse<TaskPickupDispatchDTO> findTaskPickupDispatchJobByPage(@RequestBody TaskPickupDispatchQueryDTO dto);

    /**
     * 分页查询运单信息
     *
     * @param dto 查询参数
     * @return 运单分页数据
     */
    @PostMapping("webManager/transportOrder/page")
    PageResponse<TransportOrderDTO> findTransportOrderByPage(@RequestBody TransportOrderQueryDTO dto);

    /**
     * 分页查询运输任务信息
     *
     * @param dto 查询参数
     * @return 运输任务分页数据
     */
    @PostMapping("webManager/taskTransport/page")
    PageResponse<TaskTransportDTO> findTaskTransportByPage(@RequestBody TaskTransportQueryDTO dto);
}
