package com.itheima.pinda.feign;

import com.itheima.pinda.DTO.TaskTransportDTO;
import com.itheima.pinda.common.utils.PageResponse;
import com.itheima.pinda.feign.fallback.TransportTaskFeignFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 注意：不要在接口上加类级 @RequestMapping。否则 Feign 代理与 fallback @Component 都会被
// Spring MVC 注册相同路径，启动时报 Ambiguous mapping。路径前缀下沉到每个方法。
@FeignClient(value = "pd-work", fallback = TransportTaskFeignFallback.class)
public interface TransportTaskFeign {
    /**
     * 新增运输任务
     *
     * @param dto 运输任务信息
     * @return 运输任务信息
     */
    @PostMapping("/transport-task")
    TaskTransportDTO save(@RequestBody TaskTransportDTO dto);

    /**
     * 修改运输任务信息
     *
     * @param id  运输任务id
     * @param dto 运输任务信息
     * @return 运输任务信息
     */
    @PutMapping("/transport-task/{id}")
    TaskTransportDTO updateById(@PathVariable(name = "id") Long id, @RequestBody TaskTransportDTO dto);

    /**
     * 获取运输任务分页数据
     *
     * @param dto 查询参数
     * @return 运输任务分页数据
     */
    @PostMapping("/transport-task/page")
    PageResponse<TaskTransportDTO> findByPage(@RequestBody TaskTransportDTO dto);

    /**
     * 根据id获取运输任务信息
     *
     * @param id 运输任务id
     * @return 运输任务信息
     */
    @GetMapping("/transport-task/{id}")
    TaskTransportDTO findById(@PathVariable(name = "id") Long id);

    /**
     * 获取运单列表
     *
     * @param dto 查询条件
     * @return 运单列表
     */
    @PostMapping("/transport-task/list")
    List<TaskTransportDTO> findAll(@RequestBody TaskTransportDTO dto);

    /**
     * 根据运单id或运输任务id获取运输任务列表
     *
     * @param transportOrderId 运单id
     * @param taskTransportId  运输任务id
     * @return 运输任务列表
     */
    @GetMapping("/transport-task/listByOrderIdOrTaskId")
    List<TaskTransportDTO> findAllByOrderIdOrTaskId(@RequestParam(name = "transportOrderId", required = false) Long transportOrderId,
                                                    @RequestParam(name = "taskTransportId", required = false) Long taskTransportId);
}
