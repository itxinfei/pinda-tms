package com.xinfei.feign;

import com.xinfei.DTO.TaskTransportDTO;
import com.xinfei.DTO.dashboard.TaskDashboardSummaryDTO;
import com.xinfei.DTO.dashboard.TripsVolumeDTO;
import com.xinfei.common.utils.PageResponse;
import com.xinfei.feign.fallback.TransportTaskFeignFallback;
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

    /**
     * 大屏/仪表盘运输域汇总：今日运单、今日任务、在途/待执行车辆、任务五态计数。
     *
     * @return 运输域汇总统计
     */
    @GetMapping("/transport-task/dashboard/summary")
    TaskDashboardSummaryDTO dashboardSummary();

    /**
     * 近 N 日各车次运输任务量（大屏「各线路运量」柱状图中间数据，线路名由 BFF 补全）。
     *
     * @param days 统计天数（服务端夹紧到 1~30，默认 7）
     * @return 车次任务量列表，按任务量倒序
     */
    @GetMapping("/transport-task/dashboard/trips-volume")
    List<TripsVolumeDTO> dashboardTripsVolume(@RequestParam(name = "days", defaultValue = "7") Integer days);
}
