package com.itheima.pinda.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.pinda.DTO.OrderDTO;
import com.itheima.pinda.DTO.TaskTransportDTO;
import com.itheima.pinda.DTO.TransportOrderDTO;
import com.itheima.pinda.common.CustomIdGenerator;
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
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
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

    @Autowired
    private IStatusTransitionHistoryService statusTransitionHistoryService;

    /**
     * 获取当前操作人ID，HTTP上下文为空时返回 "system"
     */
    private String getCurrentOperatorId() {
        String userId = RequestContext.getUserId();
        return userId != null ? userId : "system";
    }

    /**
     * 获取当前操作人名称，HTTP上下文为空或缺少名称头时返回 "system"
     */
    private String getCurrentOperatorName() {
        String userName = RequestContext.getUserName();
        return userName != null ? userName : "system";
    }

    /**
     * 岗位ID常量（取自网关透传的 stationid，与鉴权中心 StaticStation 保持一致）
     */
    private static final Long STATION_DRIVER = 2L;
    private static final Long STATION_COURIER = 3L;

    /**
     * 业务类型-运输任务（对应状态流转历史表 businessType 字段：1-订单，2-运单，3-运输任务）
     */
    private static final Integer BUSINESS_TYPE_TRANSPORT_TASK = 3;

    /**
     * 获取当前操作人类型（operatorType）
     *
     * <p>取值含义：1-客户 2-快递员 3-司机 4-系统 5-管理员。
     * 依据网关透传的 stationid 映射：司机岗(2)→司机(3)，快递员岗(3)→快递员(2)；
     * 其余已认证内部人员（如管理员）归为管理员(5)；无 HTTP 上下文（异步/定时任务）归为系统(4)。</p>
     */
    private Integer getCurrentOperatorType() {
        Long stationId = RequestContext.getStationId();
        if (stationId == null) {
            return 4; // 系统
        }
        if (STATION_DRIVER.equals(stationId)) {
            return 3; // 司机
        }
        if (STATION_COURIER.equals(stationId)) {
            return 2; // 快递员
        }
        return 5; // 管理员（其余内部人员）
    }

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

    /**
     * 保存运输任务并关联运单（事务保护）
     *
     * @param taskTransport 运输任务
     * @param transportOrderIds 关联的运单ID列表
     * @return 保存后的运输任务
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskTransport saveWithRelations(TaskTransport taskTransport, List<String> transportOrderIds) {
        taskTransport.setId(idGenerator.nextId(taskTransport) + "");
        taskTransport.setCreateTime(LocalDateTime.now());
        taskTransport.setStatus(TransportTaskStatus.PENDING.getCode());
        taskTransport.setAssignedStatus(TransportTaskAssignedStatus.TO_BE_DISTRIBUTED.getCode());
        taskTransport.setLoadingStatus(TransportTaskLoadingStatus.EMPTY.getCode());
        save(taskTransport);

        if (transportOrderIds != null && !transportOrderIds.isEmpty()) {
            List<TransportOrderTask> transportOrderTaskList = transportOrderIds.stream().map(transportOrderId -> {
                TransportOrderTask transportOrderTask = new TransportOrderTask();
                transportOrderTask.setTransportOrderId(transportOrderId);
                transportOrderTask.setTransportTaskId(taskTransport.getId());
                return transportOrderTask;
            }).collect(Collectors.toList());
            transportOrderTaskService.batchSaveTransportOrder(transportOrderTaskList);
        }

        return taskTransport;
    }

    /**
     * 更新运输任务并重新关联运单（事务保护）
     *
     * @param id 运输任务ID
     * @param dto 运输任务DTO
     * @param transportOrderIds 关联的运单ID列表
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateWithRelations(String id, TaskTransportDTO dto, List<String> transportOrderIds) {
        dto.setId(id);
        TaskTransport taskTransport = new TaskTransport();
        BeanUtils.copyProperties(dto, taskTransport);
        boolean updated = updateById(taskTransport);
        if (!updated) {
            return false;
        }

        // 删除旧关联关系
        transportOrderTaskService.del(null, id);

        // 保存新关联关系
        if (transportOrderIds != null && !transportOrderIds.isEmpty()) {
            List<TransportOrderTask> transportOrderTaskList = transportOrderIds.stream().map(transportOrderId -> {
                TransportOrderTask transportOrderTask = new TransportOrderTask();
                transportOrderTask.setTransportOrderId(transportOrderId);
                transportOrderTask.setTransportTaskId(id);
                return transportOrderTask;
            }).collect(Collectors.toList());
            transportOrderTaskService.batchSaveTransportOrder(transportOrderTaskList);
        }

        return true;
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
        Integer targetStatus = TransportTaskStatus.PROCESSING.getCode();
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
        if (result) {
            String operatorId = getCurrentOperatorId();
            statusTransitionHistoryService.recordTransition(
                BUSINESS_TYPE_TRANSPORT_TASK, id, id,
                taskTransport.getStatus(), targetStatus,
                operatorId, getCurrentOperatorName(), getCurrentOperatorType(), "发车确认"
            );
        }
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
        Integer targetStatus = TransportTaskStatus.CONFIRM.getCode();
        if (!stateTransitionValidator.validateTransportTaskTransition(taskTransport.getStatus(), targetStatus)) {
            log.error("运输任务[{}]状态流转非法：当前状态[{}]不能流转到[{}]",
                id, taskTransport.getStatus(), targetStatus);
            return false;
        }

        // 到达确认：状态从进行中(2)→待确认(3)
        LambdaUpdateWrapper<TaskTransport> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(TaskTransport::getId, id)
                .eq(TaskTransport::getStatus, TransportTaskStatus.PROCESSING.getCode())
                .set(TaskTransport::getStatus, targetStatus)
                .set(TaskTransport::getActualArrivalTime, LocalDateTime.now())
                .set(TaskTransport::getUpdateTime, LocalDateTime.now());
        boolean result = update(wrapper);
        if (result) {
            String operatorId = getCurrentOperatorId();
            statusTransitionHistoryService.recordTransition(
                BUSINESS_TYPE_TRANSPORT_TASK, id, id,
                taskTransport.getStatus(), targetStatus,
                operatorId, getCurrentOperatorName(), getCurrentOperatorType(), "到达确认"
            );
        }
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
                .eq(TaskTransport::getStatus, TransportTaskStatus.CONFIRM.getCode())
                .set(TaskTransport::getStatus, targetStatus)
                .set(TaskTransport::getActualDeliveryTime, LocalDateTime.now())
                .set(TaskTransport::getUpdateTime, LocalDateTime.now());
        boolean result = update(wrapper);
        if (result) {
            String operatorId = getCurrentOperatorId();
            statusTransitionHistoryService.recordTransition(
                BUSINESS_TYPE_TRANSPORT_TASK, id, id,
                taskTransport.getStatus(), targetStatus,
                operatorId, getCurrentOperatorName(), getCurrentOperatorType(), "交付确认"
            );
        }
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
                try {
                    transportOrderFeign.updateById(transportOrderId, orderDTO);
                    log.info("更新运单[{}]状态为: 到达终端网点({})", transportOrderId, TransportOrderStatus.ARRIVED_END.getCode());
                } catch (Exception e) {
                    // 运单状态流转校验或远程调用失败时不影响整体交付流程，仅记录告警
                    log.warn("更新运单[{}]状态为到达终端网点失败（可能状态流转不合法或远程调用异常）", transportOrderId, e);
                }
            });

            // 6. 批量更新所有关联订单状态为"网点出库"
            // 干线运输任务交付仅代表货物到达终端网点，还需经过末端派送（接件→妥投）才能签收，
            // 因此此处不能直接置为"已签收"，否则会跳过网点出库→待派送→派送中的末端流程。
            // 订单最终签收/拒收由快递员妥投（CourierController.delivered）确认。
            int successCount = 0;
            int failCount = 0;
            for (String transportOrderId : transportOrderIds) {
                TransportOrderDTO transportOrder = transportOrderFeign.findById(transportOrderId);
                if (transportOrder != null && StringUtils.isNotBlank(transportOrder.getOrderId())) {
                    try {
                        OrderDTO orderDTO = new OrderDTO();
                        orderDTO.setId(transportOrder.getOrderId());
                        orderDTO.setStatus(OrderStatus.OUTLETS_EX_WAREHOUSE.getCode());
                        OrderDTO updated = orderFeign.updateById(transportOrder.getOrderId(), orderDTO);
                        if (updated != null) {
                            log.info("更新订单[{}]状态为网点出库({}), 关联运单[{}]",
                                transportOrder.getOrderId(), OrderStatus.OUTLETS_EX_WAREHOUSE.getCode(), transportOrderId);
                            successCount++;
                        } else {
                            log.warn("更新订单[{}]状态为网点出库失败（可能状态流转不合法），关联运单[{}]",
                                transportOrder.getOrderId(), transportOrderId);
                            failCount++;
                        }
                    } catch (Exception e) {
                        log.error("更新订单[{}]状态失败", transportOrder.getOrderId(), e);
                        failCount++;
                    }
                } else {
                    log.warn("运单[{}]未关联有效订单，跳过状态更新", transportOrderId);
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
