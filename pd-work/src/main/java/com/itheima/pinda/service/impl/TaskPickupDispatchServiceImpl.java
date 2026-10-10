package com.itheima.pinda.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.itheima.pinda.entity.TaskPickupDispatch;
import com.itheima.pinda.enums.pickuptask.PickupDispatchTaskAssignedStatus;
import com.itheima.pinda.enums.pickuptask.PickupDispatchTaskStatus;
import com.itheima.pinda.mapper.TaskPickupDispatchMapper;
import com.itheima.pinda.service.ITaskPickupDispatchService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 取件、派件任务信息表 服务实现类
 */
@Service
public class TaskPickupDispatchServiceImpl
        extends ServiceImpl<TaskPickupDispatchMapper, TaskPickupDispatch>
        implements ITaskPickupDispatchService {

    @Override
    public TaskPickupDispatch saveTaskPickupDispatch(TaskPickupDispatch taskPickupDispatch) {
        // Long 雪花主键由 @TableId(ASSIGN_ID) 自动生成
        taskPickupDispatch.setCreateTime(LocalDateTime.now());
        taskPickupDispatch.setStatus(PickupDispatchTaskStatus.PENDING.getCode());
        taskPickupDispatch.setAssignedStatus(PickupDispatchTaskAssignedStatus.TO_BE_DISTRIBUTED.getCode());
        save(taskPickupDispatch);
        return taskPickupDispatch;
    }

    @Override
    public IPage<TaskPickupDispatch> findByPage(Integer page, Integer pageSize,
                                                TaskPickupDispatch dispatch) {
        Page<TaskPickupDispatch> iPage = new Page<>(page, pageSize);
        LambdaQueryWrapper<TaskPickupDispatch> wrapper = new LambdaQueryWrapper<>();
        if (dispatch.getCourierId() != null) {
            wrapper.eq(TaskPickupDispatch::getCourierId, dispatch.getCourierId());
        }
        if (dispatch.getAssignedStatus() != null) {
            wrapper.eq(TaskPickupDispatch::getAssignedStatus, dispatch.getAssignedStatus());
        }
        if (dispatch.getTaskType() != null) {
            wrapper.eq(TaskPickupDispatch::getTaskType, dispatch.getTaskType());
        }
        if (dispatch.getStatus() != null) {
            wrapper.eq(TaskPickupDispatch::getStatus, dispatch.getStatus());
        }
        wrapper.orderByDesc(TaskPickupDispatch::getId);
        return page(iPage, wrapper);
    }

    @Override
    public List<TaskPickupDispatch> findAll(List<Long> ids, List<Long> orderIds,
                                            TaskPickupDispatch dispatch) {
        LambdaQueryWrapper<TaskPickupDispatch> wrapper = new LambdaQueryWrapper<>();
        if (ids != null && !ids.isEmpty()) {
            wrapper.in(TaskPickupDispatch::getId, ids);
        }
        if (orderIds != null && !orderIds.isEmpty()) {
            wrapper.in(TaskPickupDispatch::getOrderId, orderIds);
        }
        if (dispatch.getAssignedStatus() != null) {
            wrapper.eq(TaskPickupDispatch::getAssignedStatus, dispatch.getAssignedStatus());
        }
        if (dispatch.getTaskType() != null) {
            wrapper.eq(TaskPickupDispatch::getTaskType, dispatch.getTaskType());
        }
        if (dispatch.getStatus() != null) {
            wrapper.eq(TaskPickupDispatch::getStatus, dispatch.getStatus());
        }
        if (dispatch.getOrderId() != null) {
            wrapper.eq(TaskPickupDispatch::getOrderId, dispatch.getOrderId());
        }
        wrapper.orderByDesc(TaskPickupDispatch::getId);
        return list(wrapper);
    }
}
