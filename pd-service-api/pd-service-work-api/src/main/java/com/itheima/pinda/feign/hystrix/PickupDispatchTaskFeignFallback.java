package com.itheima.pinda.feign.hystrix;

import com.itheima.pinda.DTO.TaskPickupDispatchDTO;
import com.itheima.pinda.common.utils.PageResponse;
import com.itheima.pinda.common.utils.Result;
import com.itheima.pinda.feign.PickupDispatchTaskFeign;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 取派件任务Feign接口熔断降级
 */
@Slf4j
@Component
public class PickupDispatchTaskFeignFallback implements PickupDispatchTaskFeign {

    @Override
    public TaskPickupDispatchDTO save(TaskPickupDispatchDTO dto) {
        log.error("远程调用 pd-work 失败: save({}), 返回null", dto);
        return null;
    }

    @Override
    public TaskPickupDispatchDTO updateById(String id, TaskPickupDispatchDTO dto) {
        log.error("远程调用 pd-work 失败: updateById({}), 返回null", id);
        return null;
    }

    @Override
    public PageResponse<TaskPickupDispatchDTO> findByPage(TaskPickupDispatchDTO dto) {
        log.error("远程调用 pd-work 失败: findByPage({}), 返回空分页", dto);
        PageResponse<TaskPickupDispatchDTO> result = new PageResponse<>();
        result.setCounts(0L);
        result.setPagesize(0);
        result.setPages(0L);
        result.setPage(0);
        result.setItems(Collections.emptyList());
        return result;
    }

    @Override
    public TaskPickupDispatchDTO findById(String id) {
        log.error("远程调用 pd-work 失败: findById({}), 返回null", id);
        return null;
    }

    @Override
    public List<TaskPickupDispatchDTO> findAll(TaskPickupDispatchDTO dto) {
        log.error("远程调用 pd-work 失败: findAll({}), 返回空列表", dto);
        return Collections.emptyList();
    }

    @Override
    public TaskPickupDispatchDTO findByOrderId(String orderId, Integer taskType) {
        log.error("远程调用 pd-work 失败: findByOrderId(orderId={}, taskType={}), 返回null", orderId, taskType);
        return null;
    }
}
