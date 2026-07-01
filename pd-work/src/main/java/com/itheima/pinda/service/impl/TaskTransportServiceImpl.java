package com.itheima.pinda.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.pinda.DTO.TaskTransportDTO;
import com.itheima.pinda.DTO.TransportOrderDTO;
import com.itheima.pinda.common.CustomIdGenerator;
import com.itheima.pinda.entity.TaskTransport;
import com.itheima.pinda.entity.TransportOrderTask;
import com.itheima.pinda.enums.OrderStatus;
import com.itheima.pinda.enums.transportorder.TransportOrderSchedulingStatus;
import com.itheima.pinda.enums.transportorder.TransportOrderStatus;
import com.itheima.pinda.enums.transporttask.TransportTaskAssignedStatus;
import com.itheima.pinda.enums.transporttask.TransportTaskLoadingStatus;
import com.itheima.pinda.enums.transporttask.TransportTaskStatus;
import com.itheima.pinda.feign.OrderFeign;
import com.itheima.pinda.feign.TransportOrderFeign;
import com.itheima.pinda.mapper.TaskTransportMapper;
import com.itheima.pinda.service.ITaskTransportService;
import com.itheima.pinda.service.ITransportOrderTaskService;
import com.itheima.pinda.state.StateTransitionValidator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 运输任务表 服务实现类
 * </p>
 *
 * @author jpf
 * @since 2019-12-29
 */
@Slf4j
@Service
public class TaskTransportServiceImpl extends
        ServiceImpl<TaskTransportMapper, TaskTransport> implements ITaskTransportService {
    @Autowired
    private CustomIdGenerator idGenerator;

    @Autowired
    private TransportOrderFeign transportOrderFeign;

    @Autowired
    private OrderFeign orderFeign;

    @Autowired
    private ITransportOrderTaskService transportOrderTaskService;

    @Autowired
    private StateTransitionValidator stateTransitionValidator;

    @Override
    public TaskTransport saveTaskTransport(TaskTransport taskTransport) {
        taskTransport.setId(idGenerator.nextId(taskTransport) + "");
        taskTransport.setCreateTime(LocalDateTime.now());
        taskTransport.setStatus(TransportTaskStatus.PENDING.getCode());
        taskTransport.setAssignedStatus(TransportTaskAssignedStatus.TO_BE_DISTRIBUTED.getCode());
        taskTransport.setLoadingStatus(TransportTaskLoadingStatus.EMPTY.getCode());
        save(taskTransport);
        return taskTransport;
    }

    @Override
    public IPage<TaskTransport> findByPage(Integer page, Integer pageSize, String id, Integer status) {
        Page<TaskTransport> iPage = new Page(page, pageSize);
        LambdaQueryWrapper<TaskTransport> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotEmpty(id)) {
            lambdaQueryWrapper.like(TaskTransport::getId, id);
        }
        if (status != null) {
            lambdaQueryWrapper.eq(TaskTransport::getStatus, status);
        }
        return page(iPage, lambdaQueryWrapper);
    }

    @Override
    public List<TaskTransport> findAll(List<String> ids, String id, Integer status, TaskTransportDTO dto) {
        LambdaQueryWrapper<TaskTransport> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        if (ids != null && ids.size() > 0) {
            lambdaQueryWrapper.in(TaskTransport::getId, ids);
        }
        if (StringUtils.isNotEmpty(id)) {
            lambdaQueryWrapper.like(TaskTransport::getId, id);
        }
        if (status != null) {
            lambdaQueryWrapper.eq(TaskTransport::getStatus, status);
        }
        if (dto != null) {
            lambdaQueryWrapper.eq(StringUtils.isNotBlank(dto.getTruckId()), TaskTransport::getTruckId, dto.getTruckId());
        }
        return list(lambdaQueryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean depart(String id) {
        // 参数校验
        if (StringUtils.isBlank(id)) {
            log.warn("发车确认失败：运输任务ID为空");
            return false;
        }

        // 获取当前运输任务
        TaskTransport taskTransport = getById(id);
        if (taskTransport == null) {
            log.warn("运输任务[{}]不存在", id);
            return false;
        }

        // 状态流转校验
        Integer targetStatus = TransportTaskStatus.IN_PROGRESS.getCode();
        if (!stateTransitionValidator.validateTransportTaskTransition(taskTransport.getStatus(), targetStatus)) {
            log.error("运输任务[{}]状态流转非法：当前状态[{}]不能流转到[{}]",
                id, taskTransport.getStatus(), targetStatus);
            return false;
        }

        // 发车确认：状态从待执行(1)→进行中(2)
        LambdaUpdateWrapper<TaskTransport> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(TaskTransport::getId, id)
                .eq(TaskTransport::getStatus, TransportTaskStatus.PENDING.getCode())
                .set(TaskTransport::getStatus, targetStatus)
                .set(TaskTransport::getActualDepartureTime, LocalDateTime.now())
                .set(TaskTransport::getUpdateTime, LocalDateTime.now());
        boolean result = update(wrapper);
        log.info("运输任务[{}]发车确认结果: {}", id, result ? "成功" : "失败");
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean arrive(String id) {
        // 参数校验
        if (StringUtils.isBlank(id)) {
            log.warn("到达确认失败：运输任务ID为空");
            return false;
        }

        // 获取当前运输任务
        TaskTransport taskTransport = getById(id);
        if (taskTransport == null) {
            log.warn("运输任务[{}]不存在", id);
            return false;
        }

        // 状态流转校验
        Integer targetStatus = TransportTaskStatus.WAITING_CONFIRM.getCode();
        if (!stateTransitionValidator.validateTransportTaskTransition(taskTransport.getStatus(), targetStatus)) {
            log.error("运输任务[{}]状态流转非法：当前状态[{}]不能流转到[{}]",
                id, taskTransport.getStatus(), targetStatus);
            return false;
        }

        // 到达确认：状态从进行中(2)→待确认(3)
        LambdaUpdateWrapper<TaskTransport> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(TaskTransport::getId, id)
                .eq(TaskTransport::getStatus, TransportTaskStatus.IN_PROGRESS.getCode())
                .set(TaskTransport::getStatus, targetStatus)
                .set(TaskTransport::getActualArrivalTime, LocalDateTime.now())
                .set(TaskTransport::getUpdateTime, LocalDateTime.now());
        boolean result = update(wrapper);
        log.info("运输任务[{}]到达确认结果: {}", id, result ? "成功" : "失败");
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deliver(String id) {
        // 参数校验
        if (StringUtils.isBlank(id)) {
            log.warn("交付确认失败：运输任务ID为空");
            return false;
        }

        // 获取当前运输任务
        TaskTransport taskTransport = getById(id);
        if (taskTransport == null) {
            log.warn("运输任务[{}]不存在", id);
            return false;
        }

        // 状态流转校验
        Integer targetStatus = TransportTaskStatus.COMPLETED.getCode();
        if (!stateTransitionValidator.validateTransportTaskTransition(taskTransport.getStatus(), targetStatus)) {
            log.error("运输任务[{}]状态流转非法：当前状态[{}]不能流转到[{}]",
                id, taskTransport.getStatus(), targetStatus);
            return false;
        }

        // 交付确认：状态从待确认(3)→已完成(4)
        LambdaUpdateWrapper<TaskTransport> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(TaskTransport::getId, id)
                .eq(TaskTransport::getStatus, TransportTaskStatus.WAITING_CONFIRM.getCode())
                .set(TaskTransport::getStatus, targetStatus)
                .set(TaskTransport::getActualDeliveryTime, LocalDateTime.now())
                .set(TaskTransport::getUpdateTime, LocalDateTime.now());
        boolean result = update(wrapper);
        log.info("运输任务[{}]交付确认结果: {}", id, result ? "成功" : "失败");

        // 如果交付成功，触发状态同步
        if (result) {
            try {
                syncStatusOnComplete(id);
            } catch (Exception e) {
                log.error("运输任务[{}]交付后状态同步失败", id, e);
                // 不抛出异常，避免影响交付流程
            }
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean syncStatusOnComplete(String id) {
        // 1. 参数校验
        if (StringUtils.isBlank(id)) {
            log.warn("状态同步失败：运输任务ID为空");
            return false;
        }

        // 2. 获取运输任务信息
        TaskTransport taskTransport = getById(id);
        if (taskTransport == null) {
            log.warn("运输任务[{}]不存在，无法同步状态", id);
            return false;
        }

        // 3. 检查是否是已完成状态
        if (!TransportTaskStatus.COMPLETED.getCode().equals(taskTransport.getStatus())) {
            log.warn("运输任务[{}]未完成，无法同步状态，当前状态: {}", id, taskTransport.getStatus());
            return false;
        }

        try {
            // 4. 查询关联的运单ID列表
            // 注意：通过 TransportOrderTask 中间表查询关联的运单ID
            LambdaQueryWrapper<TransportOrderTask> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TransportOrderTask::getTransportTaskId, id);
            List<TransportOrderTask> transportOrderTaskList = transportOrderTaskService.list(queryWrapper);

            if (transportOrderTaskList == null || transportOrderTaskList.isEmpty()) {
                log.warn("运输任务[{}]未关联运单，无需同步", id);
                return true;
            }

            List<String> transportOrderIds = transportOrderTaskList.stream()
                    .map(TransportOrderTask::getTransportOrderId)
                    .collect(Collectors.toList());

            log.info("运输任务[{}]关联{}个运单，开始同步状态", id, transportOrderIds.size());

            // 5. 批量更新运单状态为"到达终端网点"(4)
            transportOrderIds.forEach(transportOrderId -> {
                TransportOrderDTO orderDTO = new TransportOrderDTO();
                orderDTO.setStatus(TransportOrderStatus.ARRIVED_END.getCode());
                orderDTO.setSchedulingStatus(TransportOrderSchedulingStatus.SCHEDULED.getCode());
                transportOrderFeign.updateById(transportOrderId, orderDTO);
                log.info("更新运单[{}]状态为: 到达终端网点({})", transportOrderId, TransportOrderStatus.ARRIVED_END.getCode());
            });

            // 6. 更新订单状态为已签收
            // 注意：需要通过运单查询关联的订单ID，然后更新订单状态
            // 这里需要确认：一个运输任务下的运单是否都属于同一个订单？
            // 还是需要遍历每个运单，找到对应的订单并更新
            // TODO: 确认业务逻辑后完善订单状态更新
            // 目前方案：先查询第一个运单对应的订单，然后更新
            if (!transportOrderIds.isEmpty()) {
                TransportOrderDTO firstTransportOrder = transportOrderFeign.getById(transportOrderIds.get(0));
                if (firstTransportOrder != null && StringUtils.isNotBlank(firstTransportOrder.getOrderId())) {
                    OrderDTO orderDTO = new OrderDTO();
                    orderDTO.setStatus(OrderStatus.RECEIVED.getCode());
                    orderFeign.updateById(firstTransportOrder.getOrderId(), orderDTO);
                    log.info("更新订单[{}]状态为已签收({})", firstTransportOrder.getOrderId(), OrderStatus.RECEIVED.getCode());
                }
            }

            log.info("运输任务[{}]状态同步完成，成功更新{}个运单", id, transportOrderIds.size());
            return true;

        } catch (Exception e) {
            log.error("运输任务[{}]状态同步失败", id, e);
            return false;
        }
    }
}
