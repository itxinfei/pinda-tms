package com.xinfei.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xinfei.entity.Order;
import com.xinfei.entity.OrderException;
import com.xinfei.mapper.OrderExceptionMapper;
import com.xinfei.service.IOrderExceptionService;
import com.xinfei.service.IOrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 订单异常记录服务实现（ORD-04）
 *
 * <p>口径：不新增订单状态值，异常以独立表标记+留痕；
 * 订单本体状态仍由 OrderStatusTransitions 状态机管控。</p>
 */
@Slf4j
@Service
public class OrderExceptionServiceImpl extends ServiceImpl<OrderExceptionMapper, OrderException>
        implements IOrderExceptionService {

    /**
     * 异常类型：1滞留 2破损 3退回 4其他（禁硬编码状态数字口径的枚举值范围校验）
     */
    private static final int TYPE_MIN = 1;
    private static final int TYPE_MAX = 4;

    @Autowired
    @Lazy
    private IOrderService orderService;

    @Override
    public OrderException register(Long orderId, Integer exceptionType, String description, Long operatorId) {
        if (orderId == null) {
            throw new IllegalArgumentException("订单ID不能为空");
        }
        if (exceptionType == null || exceptionType < TYPE_MIN || exceptionType > TYPE_MAX) {
            throw new IllegalArgumentException("异常类型非法（1滞留 2破损 3退回 4其他）");
        }
        if (StringUtils.isBlank(description)) {
            throw new IllegalArgumentException("异常描述不能为空");
        }
        if (description.length() > 500) {
            throw new IllegalArgumentException("异常描述不能超过500字");
        }
        Order order = orderService.getById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        OrderException entity = new OrderException();
        entity.setOrderId(orderId);
        entity.setExceptionType(exceptionType);
        entity.setDescription(description.trim());
        entity.setHandleStatus(1);
        entity.setCreateBy(operatorId);
        entity.setCreateTime(LocalDateTime.now());
        save(entity);
        log.info("[订单异常] 登记成功: orderId={}, type={}, id={}", orderId, exceptionType, entity.getId());
        return entity;
    }

    @Override
    public boolean handle(Long id, String handleResult, String operatorName) {
        if (id == null) {
            return false;
        }
        if (StringUtils.isBlank(handleResult)) {
            throw new IllegalArgumentException("处理结果不能为空");
        }
        if (handleResult.length() > 500) {
            throw new IllegalArgumentException("处理结果不能超过500字");
        }
        OrderException existing = getById(id);
        if (existing == null) {
            log.warn("[订单异常] 处理失败：记录不存在 id={}", id);
            return false;
        }
        // 幂等保护：已处理记录拒绝重复处理
        if (existing.getHandleStatus() != null && existing.getHandleStatus() == 2) {
            log.warn("[订单异常] 记录已处理，拒绝重复处理: id={}", id);
            return false;
        }
        OrderException update = new OrderException();
        update.setId(id);
        update.setHandleStatus(2);
        update.setHandleResult(handleResult.trim());
        update.setHandlerUser(StringUtils.defaultIfBlank(operatorName, "系统"));
        update.setHandleTime(LocalDateTime.now());
        boolean ok = updateById(update);
        if (ok) {
            log.info("[订单异常] 处理完成: id={}, handler={}", id, update.getHandlerUser());
        }
        return ok;
    }

    @Override
    public IPage<OrderException> findByPage(Integer page, Integer pageSize, Long orderId,
                                            Integer exceptionType, Integer handleStatus) {
        Page<OrderException> iPage = new Page<>(page == null ? 1 : page, pageSize == null ? 10 : pageSize);
        LambdaQueryWrapper<OrderException> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(orderId != null, OrderException::getOrderId, orderId);
        wrapper.eq(exceptionType != null, OrderException::getExceptionType, exceptionType);
        wrapper.eq(handleStatus != null, OrderException::getHandleStatus, handleStatus);
        wrapper.orderByDesc(OrderException::getCreateTime);
        return page(iPage, wrapper);
    }
}
