package com.itheima.pinda.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.itheima.pinda.DTO.OrderDTO;
import com.itheima.pinda.DTO.TaskTransportDTO;
import com.itheima.pinda.DTO.TransportOrderDTO;
import com.itheima.pinda.common.context.RequestContext;
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
import com.itheima.pinda.service.state.IStatusTransitionHistoryService;
import com.itheima.pinda.state.StateTransitionValidator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 干线运输任务表 服务实现类
 */
@Slf4j
@Service
public class TaskTransportServiceImpl extends
        ServiceImpl<TaskTransportMapper, TaskTransport> implements ITaskTransportService {

    @Autowired
    private TransportOrderFeign transportOrderFeign;

    @Autowired
    private OrderFeign orderFeign;

    @Autowired
    private ITransportOrderTaskService transportOrderTaskService;

    @Autowired
    private StateTransitionValidator stateTransitionValidator;

    @Autowired
    private IStatusTransitionHistoryService statusTransitionHistoryService;

    /**
     * 岗位ID常量（网关透传 stationid）
     */
    private static final Long STATION_DRIVER = 2L;
    private static final Long STATION_COURIER = 3L;

    /**
     * 业务类型-运输任务（work_status_transition_history.business_type：1运输任务）
     */
    private static final Integer BUSINESS_TYPE_TRANSPORT_TASK = 1;

    /**
     * 当前操作人ID：解析 userid 头为 Long；系统态无上下文时返回 null
     */
    private Long getCurrentOperatorId() {
        String userId = RequestContext.getUserId();
        if (StringUtils.isNotBlank(userId)) {
            try {
                return Long.parseLong(userId.trim());
            } catch (NumberFormatException ignored) {
                // 非法头值，返回 null
            }
        }
        return null;
    }

    /**
     * 当前操作人姓名：缺省 "system"
     */
    private String getCurrentOperatorName() {
        String userName = RequestContext.getUserName();
        return userName != null ? userName : "system";
    }

    /**
     * 当前操作人类型（operator_type）：1后台 2司机 3快递员；
     * 依据 stationid 映射：司机岗(2)→2，快递员岗(3)→3，其余内部人员→1；
     * 无 HTTP 上下文（异步/定时）返回 null。
     */
    private Integer getCurrentOperatorType() {
        Long stationId = RequestContext.getStationId();
        if (stationId == null) {
            return null;
        }
        if (STATION_DRIVER.equals(stationId)) {
            return 2;
        }
        if (STATION_COURIER.equals(stationId)) {
            return 3;
        }
        return 1;
    }

    @Override
    public TaskTransport saveTaskTransport(TaskTransport taskTransport) {
        // Long 雪花主键由 @TableId(ASSIGN_ID) 自动生成
        taskTransport.setCreateTime(LocalDateTime.now());
        taskTransport.setStatus(TransportTaskStatus.PENDING.getCode());
        taskTransport.setAssignedStatus(TransportTaskAssignedStatus.TO_BE_DISTRIBUTED.getCode());
        taskTransport.setLoadingStatus(TransportTaskLoadingStatus.EMPTY.getCode());
        save(taskTransport);
        return taskTransport;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskTransport saveWithRelations(TaskTransport taskTransport, List<Long> transportOrderIds) {
        taskTransport.setCreateTime(LocalDateTime.now());
        taskTransport.setStatus(TransportTaskStatus.PENDING.getCode());
        taskTransport.setAssignedStatus(TransportTaskAssignedStatus.TO_BE_DISTRIBUTED.getCode());
        taskTransport.setLoadingStatus(TransportTaskLoadingStatus.EMPTY.getCode());
        save(taskTransport);

        if (transportOrderIds != null && !transportOrderIds.isEmpty()) {
            List<TransportOrderTask> transportOrderTaskList = transportOrderIds.stream().map(transportOrderId -> {
                TransportOrderTask transportOrderTask = new TransportOrderTask();
                transportOrderTask.setTransportOrderId(transportOrderId);
                transportOrderTask.setTaskId(taskTransport.getId());
                return transportOrderTask;
            }).collect(Collectors.toList());
            transportOrderTaskService.batchSaveTransportOrder(transportOrderTaskList);
        }

        return taskTransport;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateWithRelations(Long id, TaskTransportDTO dto, List<Long> transportOrderIds) {
        dto.setId(id);
        TaskTransport taskTransport = new TaskTransport();
        BeanUtils.copyProperties(dto, taskTransport);
        if (!updateById(taskTransport)) {
            return false;
        }

        // 删除旧关联关系
        transportOrderTaskService.del(null, id);

        // 保存新关联关系
        if (transportOrderIds != null && !transportOrderIds.isEmpty()) {
            List<TransportOrderTask> transportOrderTaskList = transportOrderIds.stream().map(transportOrderId -> {
                TransportOrderTask transportOrderTask = new TransportOrderTask();
                transportOrderTask.setTransportOrderId(transportOrderId);
                transportOrderTask.setTaskId(id);
                return transportOrderTask;
            }).collect(Collectors.toList());
            transportOrderTaskService.batchSaveTransportOrder(transportOrderTaskList);
        }

        return true;
    }

    @Override
    public IPage<TaskTransport> findByPage(Integer page, Integer pageSize, Long id, Integer status) {
        Page<TaskTransport> iPage = new Page<>(page, pageSize);
        LambdaQueryWrapper<TaskTransport> wrapper = new LambdaQueryWrapper<>();
        if (id != null) {
            wrapper.eq(TaskTransport::getId, id);
        }
        if (status != null) {
            wrapper.eq(TaskTransport::getStatus, status);
        }
        return page(iPage, wrapper);
    }

    @Override
    public List<TaskTransport> findAll(List<Long> ids, Long id, Integer status, TaskTransportDTO dto) {
        LambdaQueryWrapper<TaskTransport> wrapper = new LambdaQueryWrapper<>();
        if (ids != null && !ids.isEmpty()) {
            wrapper.in(TaskTransport::getId, ids);
        }
        if (id != null) {
            wrapper.eq(TaskTransport::getId, id);
        }
        if (status != null) {
            wrapper.eq(TaskTransport::getStatus, status);
        }
        if (dto != null && dto.getTruckId() != null) {
            wrapper.eq(TaskTransport::getTruckId, dto.getTruckId());
        }
        return list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean depart(Long id) {
        if (id == null) {
            log.warn("发车确认失败：运输任务ID为空");
            return false;
        }

        TaskTransport taskTransport = getById(id);
        if (taskTransport == null) {
            log.warn("运输任务[{}]不存在", id);
            return false;
        }

        Integer targetStatus = TransportTaskStatus.PROCESSING.getCode();
        if (!stateTransitionValidator.validateTransportTaskTransition(taskTransport.getStatus(), targetStatus)) {
            log.error("运输任务[{}]状态流转非法：当前状态[{}]不能流转到[{}]",
                id, taskTransport.getStatus(), targetStatus);
            return false;
        }

        // 发车确认：状态 待执行(1)→进行中(2)
        LambdaUpdateWrapper<TaskTransport> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(TaskTransport::getId, id)
                .eq(TaskTransport::getStatus, TransportTaskStatus.PENDING.getCode())
                .set(TaskTransport::getStatus, targetStatus)
                .set(TaskTransport::getActualDepartureTime, LocalDateTime.now())
                .set(TaskTransport::getUpdateTime, LocalDateTime.now());
        boolean result = update(wrapper);
        if (result) {
            statusTransitionHistoryService.recordTransition(
                BUSINESS_TYPE_TRANSPORT_TASK, id, String.valueOf(id),
                taskTransport.getStatus(), targetStatus,
                getCurrentOperatorId(), getCurrentOperatorName(), getCurrentOperatorType(), "发车确认"
            );
        }
        log.info("运输任务[{}]发车确认结果: {}", id, result ? "成功" : "失败");
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean arrive(Long id) {
        if (id == null) {
            log.warn("到达确认失败：运输任务ID为空");
            return false;
        }

        TaskTransport taskTransport = getById(id);
        if (taskTransport == null) {
            log.warn("运输任务[{}]不存在", id);
            return false;
        }

        Integer targetStatus = TransportTaskStatus.CONFIRM.getCode();
        if (!stateTransitionValidator.validateTransportTaskTransition(taskTransport.getStatus(), targetStatus)) {
            log.error("运输任务[{}]状态流转非法：当前状态[{}]不能流转到[{}]",
                id, taskTransport.getStatus(), targetStatus);
            return false;
        }

        // 到达确认：状态 进行中(2)→待确认(3)
        LambdaUpdateWrapper<TaskTransport> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(TaskTransport::getId, id)
                .eq(TaskTransport::getStatus, TransportTaskStatus.PROCESSING.getCode())
                .set(TaskTransport::getStatus, targetStatus)
                .set(TaskTransport::getActualArrivalTime, LocalDateTime.now())
                .set(TaskTransport::getUpdateTime, LocalDateTime.now());
        boolean result = update(wrapper);
        if (result) {
            statusTransitionHistoryService.recordTransition(
                BUSINESS_TYPE_TRANSPORT_TASK, id, String.valueOf(id),
                taskTransport.getStatus(), targetStatus,
                getCurrentOperatorId(), getCurrentOperatorName(), getCurrentOperatorType(), "到达确认"
            );
        }
        log.info("运输任务[{}]到达确认结果: {}", id, result ? "成功" : "失败");
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deliver(Long id) {
        if (id == null) {
            log.warn("交付确认失败：运输任务ID为空");
            return false;
        }

        TaskTransport taskTransport = getById(id);
        if (taskTransport == null) {
            log.warn("运输任务[{}]不存在", id);
            return false;
        }

        Integer targetStatus = TransportTaskStatus.COMPLETED.getCode();
        if (!stateTransitionValidator.validateTransportTaskTransition(taskTransport.getStatus(), targetStatus)) {
            log.error("运输任务[{}]状态流转非法：当前状态[{}]不能流转到[{}]",
                id, taskTransport.getStatus(), targetStatus);
            return false;
        }

        // 交付确认：状态 待确认(3)→已完成(4)
        LambdaUpdateWrapper<TaskTransport> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(TaskTransport::getId, id)
                .eq(TaskTransport::getStatus, TransportTaskStatus.CONFIRM.getCode())
                .set(TaskTransport::getStatus, targetStatus)
                .set(TaskTransport::getActualDeliveryTime, LocalDateTime.now())
                .set(TaskTransport::getUpdateTime, LocalDateTime.now());
        boolean result = update(wrapper);
        if (result) {
            statusTransitionHistoryService.recordTransition(
                BUSINESS_TYPE_TRANSPORT_TASK, id, String.valueOf(id),
                taskTransport.getStatus(), targetStatus,
                getCurrentOperatorId(), getCurrentOperatorName(), getCurrentOperatorType(), "交付确认"
            );
        }
        log.info("运输任务[{}]交付确认结果: {}", id, result ? "成功" : "失败");

        // 交付成功后触发运单/订单联动
        if (result) {
            try {
                syncStatusOnComplete(id);
            } catch (Exception e) {
                log.error("运输任务[{}]交付后状态同步失败", id, e);
            }
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean syncStatusOnComplete(Long id) {
        if (id == null) {
            log.warn("状态同步失败：运输任务ID为空");
            return false;
        }

        TaskTransport taskTransport = getById(id);
        if (taskTransport == null) {
            log.warn("运输任务[{}]不存在，无法同步状态", id);
            return false;
        }

        if (!TransportTaskStatus.COMPLETED.getCode().equals(taskTransport.getStatus())) {
            log.warn("运输任务[{}]未完成，无法同步状态，当前状态: {}", id, taskTransport.getStatus());
            return false;
        }

        try {
            // 通过中间表查询关联运单（关联字段 task_id = 当前运输任务）
            LambdaQueryWrapper<TransportOrderTask> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TransportOrderTask::getTaskId, id);
            List<TransportOrderTask> links = transportOrderTaskService.list(queryWrapper);

            if (links == null || links.isEmpty()) {
                log.warn("运输任务[{}]未关联运单，无需同步", id);
                return true;
            }

            List<Long> transportOrderIds = links.stream()
                    .map(TransportOrderTask::getTransportOrderId)
                    .collect(Collectors.toList());
            log.info("运输任务[{}]关联{}个运单，开始同步状态", id, transportOrderIds.size());

            // ① 运单状态 → 到达终端网点(4)、调度状态 → 已调度(3)
            transportOrderIds.forEach(transportOrderId -> {
                TransportOrderDTO orderDTO = new TransportOrderDTO();
                orderDTO.setStatus(TransportOrderStatus.ARRIVED_END.getCode());
                orderDTO.setSchedulingStatus(TransportOrderSchedulingStatus.SCHEDULED.getCode());
                try {
                    transportOrderFeign.updateById(transportOrderId, orderDTO);
                    log.info("更新运单[{}]状态为: 到达终端网点({})",
                        transportOrderId, TransportOrderStatus.ARRIVED_END.getCode());
                } catch (Exception e) {
                    log.warn("更新运单[{}]状态为到达终端网点失败（状态流转或远程调用异常）", transportOrderId, e);
                }
            });

            // ② 订单状态 → 网点出库(7)。pd-oms 尚未切库，OrderFeign/OrderDTO 仍是 String id 契约，
            //    故此处把 Long 订单ID转成 String 再调用；最终签收/拒收由快递员妥投确认。
            int successCount = 0;
            int failCount = 0;
            for (Long transportOrderId : transportOrderIds) {
                TransportOrderDTO transportOrder = transportOrderFeign.findById(transportOrderId);
                if (transportOrder != null && transportOrder.getOrderId() != null) {
                    try {
                        String omsOrderId = String.valueOf(transportOrder.getOrderId());
                        OrderDTO orderDTO = new OrderDTO();
                        orderDTO.setId(omsOrderId);
                        orderDTO.setStatus(OrderStatus.OUTLETS_EX_WAREHOUSE.getCode());
                        OrderDTO updated = orderFeign.updateById(omsOrderId, orderDTO);
                        if (updated != null) {
                            log.info("更新订单[{}]状态为网点出库({}), 关联运单[{}]",
                                omsOrderId, OrderStatus.OUTLETS_EX_WAREHOUSE.getCode(), transportOrderId);
                            successCount++;
                        } else {
                            log.warn("更新订单[{}]状态为网点出库失败（可能状态流转不合法），关联运单[{}]",
                                omsOrderId, transportOrderId);
                            failCount++;
                        }
                    } catch (Exception e) {
                        log.error("更新订单[{}]状态失败，关联运单[{}]", transportOrder.getOrderId(), transportOrderId, e);
                        failCount++;
                    }
                } else {
                    log.warn("运单[{}]未关联有效订单，跳过订单状态更新", transportOrderId);
                    failCount++;
                }
            }
            log.info("运输任务[{}]订单状态同步完成，成功:{}, 失败:{}", id, successCount, failCount);
            return true;

        } catch (Exception e) {
            log.error("运输任务[{}]状态同步失败", id, e);
            return false;
        }
    }
}
