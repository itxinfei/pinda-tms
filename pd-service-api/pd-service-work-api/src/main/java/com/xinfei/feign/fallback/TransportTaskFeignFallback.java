package com.xinfei.feign.fallback;

import com.xinfei.DTO.TaskTransportDTO;
import com.xinfei.DTO.dashboard.NameValueDTO;
import com.xinfei.DTO.dashboard.TaskDashboardSummaryDTO;
import com.xinfei.DTO.dashboard.TripsVolumeDTO;
import com.xinfei.common.utils.PageResponse;
import com.xinfei.feign.TransportTaskFeign;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 运输任务Feign接口熔断降级
 */
@Slf4j
@Component
public class TransportTaskFeignFallback implements TransportTaskFeign {

    @Override
    public TaskTransportDTO save(TaskTransportDTO dto) {
        log.warn("远程调用 pd-work 失败: save({}), 返回null", dto);
        return null;
    }

    @Override
    public TaskTransportDTO updateById(Long id, TaskTransportDTO dto) {
        log.warn("远程调用 pd-work 失败: updateById({}), 返回null", id);
        return null;
    }

    @Override
    public PageResponse<TaskTransportDTO> findByPage(TaskTransportDTO dto) {
        log.warn("远程调用 pd-work 失败: findByPage({}), 返回空分页", dto);
        PageResponse<TaskTransportDTO> result = new PageResponse<>();
        result.setCounts(0L);
        result.setPagesize(0);
        result.setPages(0L);
        result.setPage(0);
        result.setItems(Collections.emptyList());
        return result;
    }

    @Override
    public TaskTransportDTO findById(Long id) {
        log.warn("远程调用 pd-work 失败: findById({}), 返回null", id);
        return null;
    }

    @Override
    public List<TaskTransportDTO> findAll(TaskTransportDTO dto) {
        log.warn("远程调用 pd-work 失败: findAll({}), 返回空列表", dto);
        return Collections.emptyList();
    }

    @Override
    public List<TaskTransportDTO> findAllByOrderIdOrTaskId(Long transportOrderId, Long taskTransportId) {
        log.warn("远程调用 pd-work 失败: findAllByOrderIdOrTaskId(transportOrderId={}, taskTransportId={}), 返回空列表", transportOrderId, taskTransportId);
        return Collections.emptyList();
    }

    @Override
    public TaskDashboardSummaryDTO dashboardSummary() {
        log.warn("远程调用 pd-work 失败: dashboardSummary(), 返回全 0 汇总");
        TaskDashboardSummaryDTO summary = new TaskDashboardSummaryDTO();
        summary.setTodayTransportOrderCount(0L);
        summary.setTodayTaskCount(0L);
        summary.setInTransitTruckCount(0L);
        summary.setPendingTruckCount(0L);
        // 五态固定顺序补零，前端环形图不缺扇区
        summary.setTaskStatusCounts(java.util.Arrays.asList(
                new NameValueDTO("待执行", 0L),
                new NameValueDTO("进行中", 0L),
                new NameValueDTO("待确认", 0L),
                new NameValueDTO("已完成", 0L),
                new NameValueDTO("已取消", 0L)));
        return summary;
    }

    @Override
    public List<TripsVolumeDTO> dashboardTripsVolume(Integer days) {
        log.warn("远程调用 pd-work 失败: dashboardTripsVolume(days={}), 返回空列表", days);
        return Collections.emptyList();
    }
}
