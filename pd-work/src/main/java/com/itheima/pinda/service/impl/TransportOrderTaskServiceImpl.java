package com.itheima.pinda.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.itheima.pinda.entity.TransportOrderTask;
import com.itheima.pinda.mapper.TransportOrderTaskMapper;
import com.itheima.pinda.service.ITransportOrderTaskService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 运单、运输任务关系服务实现类
 */
@Service
public class TransportOrderTaskServiceImpl extends
        ServiceImpl<TransportOrderTaskMapper, TransportOrderTask>
        implements ITransportOrderTaskService {

    @Override
    public void batchSaveTransportOrder(List<TransportOrderTask> transportOrderTaskList) {
        // Long 雪花主键由 @TableId(ASSIGN_ID) 自动生成，直接批量保存
        saveBatch(transportOrderTaskList);
    }

    @Override
    public IPage<TransportOrderTask> findByPage(Integer page, Integer pageSize,
                                                Long transportOrderId, Long taskId) {
        Page<TransportOrderTask> iPage = new Page<>(page, pageSize);
        LambdaQueryWrapper<TransportOrderTask> wrapper = new LambdaQueryWrapper<>();
        if (transportOrderId != null) {
            wrapper.eq(TransportOrderTask::getTransportOrderId, transportOrderId);
        }
        if (taskId != null) {
            wrapper.eq(TransportOrderTask::getTaskId, taskId);
        }
        return page(iPage, wrapper);
    }

    @Override
    public List<TransportOrderTask> findAll(Long transportOrderId, Long taskId) {
        LambdaQueryWrapper<TransportOrderTask> wrapper = new LambdaQueryWrapper<>();
        if (transportOrderId != null) {
            wrapper.eq(TransportOrderTask::getTransportOrderId, transportOrderId);
        }
        if (taskId != null) {
            wrapper.eq(TransportOrderTask::getTaskId, taskId);
        }
        wrapper.orderByAsc(TransportOrderTask::getId);
        return list(wrapper);
    }

    @Override
    public Integer count(Long transportOrderId, Long taskId) {
        LambdaQueryWrapper<TransportOrderTask> wrapper = new LambdaQueryWrapper<>();
        if (transportOrderId != null) {
            wrapper.eq(TransportOrderTask::getTransportOrderId, transportOrderId);
        }
        if (taskId != null) {
            wrapper.eq(TransportOrderTask::getTaskId, taskId);
        }
        // 直接走 baseMapper，避免旧代码 count(wrapper) 自调用递归
        return Math.toIntExact(baseMapper.selectCount(wrapper));
    }

    @Override
    public void del(Long transportOrderId, Long taskId) {
        if (transportOrderId == null && taskId == null) {
            throw new IllegalArgumentException("transportOrderId 和 taskId 不能同时为空");
        }
        LambdaQueryWrapper<TransportOrderTask> wrapper = new LambdaQueryWrapper<>();
        if (transportOrderId != null) {
            wrapper.eq(TransportOrderTask::getTransportOrderId, transportOrderId);
        }
        if (taskId != null) {
            wrapper.eq(TransportOrderTask::getTaskId, taskId);
        }
        remove(wrapper);
    }
}
