package com.itheima.pinda.feign.hystrix;

import com.itheima.pinda.DTO.TaskTransportDTO;
import com.itheima.pinda.common.utils.PageResponse;
import com.itheima.pinda.common.utils.Result;
import com.itheima.pinda.feign.TransportTaskFeign;
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
        log.error("远程调用 pd-work 失败: save({}), 返回null", dto);
        return null;
    }

    @Override
    public TaskTransportDTO updateById(String id, TaskTransportDTO dto) {
        log.error("远程调用 pd-work 失败: updateById({}), 返回null", id);
        return null;
    }

    @Override
    public PageResponse<TaskTransportDTO> findByPage(TaskTransportDTO dto) {
        log.error("远程调用 pd-work 失败: findByPage({}), 返回空分页", dto);
        PageResponse<TaskTransportDTO> result = new PageResponse<>();
        result.setCounts(0L);
        result.setPagesize(0);
        result.setPages(0L);
        result.setPage(0);
        result.setItems(Collections.emptyList());
        return result;
    }

    @Override
    public TaskTransportDTO findById(String id) {
        log.error("远程调用 pd-work 失败: findById({}), 返回null", id);
        return null;
    }

    @Override
    public List<TaskTransportDTO> findAll(TaskTransportDTO dto) {
        log.error("远程调用 pd-work 失败: findAll({}), 返回空列表", dto);
        return Collections.emptyList();
    }

    @Override
    public List<TaskTransportDTO> findAllByOrderIdOrTaskId(String transportOrderId, String taskTransportId) {
        log.error("远程调用 pd-work 失败: findAllByOrderIdOrTaskId(transportOrderId={}, taskTransportId={}), 返回空列表", transportOrderId, taskTransportId);
        return Collections.emptyList();
    }
}
