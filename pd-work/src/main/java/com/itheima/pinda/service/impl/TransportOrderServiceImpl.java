package com.itheima.pinda.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.pinda.common.CustomIdGenerator;
import com.itheima.pinda.entity.TransportOrder;
import com.itheima.pinda.enums.transportorder.TransportOrderSchedulingStatus;
import com.itheima.pinda.enums.transportorder.TransportOrderStatus;
import com.itheima.pinda.mapper.TransportOrderMapper;
import com.itheima.pinda.service.ITransportOrderService;
import com.itheima.pinda.state.StateTransitionValidator;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 运单服务实现类
 * </p>
 */
@Service
@Slf4j
public class TransportOrderServiceImpl extends
        ServiceImpl<TransportOrderMapper, TransportOrder> implements ITransportOrderService {
    @Autowired
    private CustomIdGenerator idGenerator;

    @Autowired
    private StateTransitionValidator stateTransitionValidator;

    @Override
    public TransportOrder saveTransportOrder(TransportOrder transportOrder) {
        transportOrder.setCreateTime(LocalDateTime.now());
        transportOrder.setId(idGenerator.nextId(transportOrder) + "");
        transportOrder.setStatus(TransportOrderStatus.CREATED.getCode());
        transportOrder.setSchedulingStatus(TransportOrderSchedulingStatus.TO_BE_SCHEDULED.getCode());
        save(transportOrder);
        return transportOrder;
    }

    /**
     * 修改运单（重写以接入状态流转校验）
     *
     * <p>所有运单状态/调度状态的变更（司机 App、快递员 App、运输任务交付同步、调度等）
     * 最终都会经过 {@code updateById}，因此在此统一校验两个独立维度的状态流转，
     * 仅当状态真正发生变化时才校验，避免普通字段更新被误拦截。</p>
     *
     * @param transportOrder 待更新的运单（需包含 id）
     * @return 是否更新成功；状态流转非法时返回 false 并记录错误日志
     */
    @Override
    public boolean updateById(TransportOrder transportOrder) {
        if (transportOrder == null || StringUtils.isBlank(transportOrder.getId())) {
            log.warn("运单更新失败：运单ID为空");
            return false;
        }

        // 读取当前持久化状态，用于校验流转合法性
        TransportOrder existing = getById(transportOrder.getId());
        if (existing == null) {
            log.warn("运单[{}]不存在，无法更新", transportOrder.getId());
            return false;
        }

        // 状态维度校验（仅在 status 发生变化时校验）
        Integer newStatus = transportOrder.getStatus();
        if (newStatus != null && !newStatus.equals(existing.getStatus())) {
            if (!stateTransitionValidator.validateTransportOrderStatusTransition(existing.getStatus(), newStatus)) {
                log.error("运单[{}]状态流转非法：当前状态[{}]不能流转到[{}]",
                    transportOrder.getId(), existing.getStatus(), newStatus);
                return false;
            }
        }

        // 调度状态维度校验（仅在 schedulingStatus 发生变化时校验）
        Integer newSchedulingStatus = transportOrder.getSchedulingStatus();
        if (newSchedulingStatus != null && !newSchedulingStatus.equals(existing.getSchedulingStatus())) {
            if (!stateTransitionValidator.validateTransportOrderSchedulingTransition(
                existing.getSchedulingStatus(), newSchedulingStatus)) {
                log.error("运单[{}]调度状态流转非法：当前调度状态[{}]不能流转到[{}]",
                    transportOrder.getId(), existing.getSchedulingStatus(), newSchedulingStatus);
                return false;
            }
        }

        return super.updateById(transportOrder);
    }

    @Override
    public IPage<TransportOrder> findByPage(Integer page, Integer pageSize, String orderId, Integer status, Integer schedulingStatus) {
        Page<TransportOrder> iPage = new Page(page, pageSize);
        LambdaQueryWrapper<TransportOrder> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotEmpty(orderId)) {
            lambdaQueryWrapper.like(TransportOrder::getOrderId, orderId);
        }
        if (status != null) {
            lambdaQueryWrapper.eq(TransportOrder::getStatus, status);
        }
        if (schedulingStatus != null) {
            lambdaQueryWrapper.eq(TransportOrder::getSchedulingStatus, schedulingStatus);
        }
        return page(iPage, lambdaQueryWrapper);
    }

    @Override
    public List<TransportOrder> findAll(List<String> ids, String orderId, Integer status, Integer schedulingStatus) {
        LambdaQueryWrapper<TransportOrder> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        if (ids != null && ids.size() > 0) {
            lambdaQueryWrapper.in(TransportOrder::getId, ids);
        }
        if (StringUtils.isNotEmpty(orderId)) {
            lambdaQueryWrapper.like(TransportOrder::getOrderId, orderId);
        }
        if (status != null) {
            lambdaQueryWrapper.eq(TransportOrder::getStatus, status);
        }
        if (schedulingStatus != null) {
            lambdaQueryWrapper.eq(TransportOrder::getSchedulingStatus, schedulingStatus);
        }
        return list(lambdaQueryWrapper);
    }

    @Override
    public TransportOrder findByOrderId(String orderId) {
        return getOne(new LambdaQueryWrapper<TransportOrder>().eq(TransportOrder::getOrderId, orderId));
    }
}
