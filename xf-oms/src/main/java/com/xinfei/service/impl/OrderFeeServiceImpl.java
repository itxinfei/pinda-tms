package com.xinfei.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xinfei.entity.FeeItem;
import com.xinfei.entity.Order;
import com.xinfei.entity.OrderFee;
import com.xinfei.mapper.OrderFeeMapper;
import com.xinfei.mapper.OrderMapper;
import com.xinfei.service.IFeeItemService;
import com.xinfei.service.IOrderFeeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单附加费明细服务实现（POD-03 第一期）
 *
 * <p>第一期明细独立存表不改写 order.amount（不破坏现有 Drools 主流程与既有对账）。</p>
 */
@Slf4j
@Service
public class OrderFeeServiceImpl extends ServiceImpl<OrderFeeMapper, OrderFee> implements IOrderFeeService {

    @Autowired
    private OrderMapper orderMapper;

    /** @Lazy 断环：OrderFeeServiceImpl → IFeeItemService */
    @Lazy
    @Autowired
    private IFeeItemService feeItemService;

    @Override
    public List<OrderFee> listByOrderId(String orderId) {
        if (StringUtils.isBlank(orderId)) {
            return new ArrayList<>();
        }
        return this.list(new LambdaQueryWrapper<OrderFee>()
                .eq(OrderFee::getOrderId, orderId)
                .orderByAsc(OrderFee::getCreateTime));
    }

    @Override
    public BigDecimal sumByOrderId(String orderId) {
        BigDecimal sum = BigDecimal.ZERO;
        for (OrderFee fee : listByOrderId(orderId)) {
            if (fee.getAmount() != null) {
                sum = sum.add(fee.getAmount());
            }
        }
        return sum;
    }

    @Override
    public OrderFee addManual(OrderFee fee, Long operatorId, String operatorName) {
        if (StringUtils.isBlank(fee.getOrderId())) {
            throw new IllegalArgumentException("订单id不能为空");
        }
        if (StringUtils.isBlank(fee.getFeeCode())) {
            throw new IllegalArgumentException("费用项编码不能为空");
        }
        if (fee.getAmount() == null || fee.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("金额必须大于 0");
        }
        FeeItem item = feeItemService.getOne(
                new LambdaQueryWrapper<FeeItem>().eq(FeeItem::getFeeCode, fee.getFeeCode()).last("LIMIT 1"));
        if (item == null) {
            throw new IllegalArgumentException("费用项不存在：" + fee.getFeeCode());
        }
        LocalDateTime now = LocalDateTime.now();
        fee.setFeeName(item.getFeeName());
        fee.setSource(OrderFee.SOURCE_MANUAL);
        fee.setOperatorId(operatorId);
        fee.setOperatorName(operatorName);
        fee.setCreateBy(operatorId);
        fee.setCreateTime(now);
        fee.setUpdateBy(operatorId);
        fee.setUpdateTime(now);
        this.save(fee);
        log.info("[附加费] 人工补录 orderId={} feeCode={} amount={} 操作人={}",
                fee.getOrderId(), fee.getFeeCode(), fee.getAmount(), operatorName);
        return fee;
    }

    @Override
    public List<OrderFee> calculateForOrder(Long orderId, Long operatorId, String operatorName) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在：" + orderId);
        }
        if (order.getAmount() == null || order.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("订单无有效运费金额，无法按比例生成附加费");
        }
        List<FeeItem> items = feeItemService.list(new LambdaQueryWrapper<FeeItem>()
                .eq(FeeItem::getStatus, FeeItem.STATUS_ENABLED));
        List<OrderFee> existing = listByOrderId(String.valueOf(orderId));
        List<OrderFee> created = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (FeeItem item : items) {
            if (existing.stream().anyMatch(f -> item.getFeeCode().equals(f.getFeeCode()))) {
                continue;
            }
            OrderFee fee = new OrderFee();
            fee.setOrderId(String.valueOf(orderId));
            fee.setFeeCode(item.getFeeCode());
            fee.setFeeName(item.getFeeName());
            fee.setSource(OrderFee.SOURCE_AUTO);
            fee.setOperatorId(operatorId);
            fee.setOperatorName(operatorName);
            if (item.getCalcType() != null && item.getCalcType() == FeeItem.CALC_TYPE_RATIO
                    && item.getRatio() != null) {
                BigDecimal amount = order.getAmount().multiply(item.getRatio()).setScale(2, RoundingMode.HALF_UP);
                fee.setAmount(amount);
                fee.setCalcBasis("运费 " + order.getAmount() + " × " + item.getRatio().stripTrailingZeros().toPlainString()
                        + " = " + amount);
            } else if (item.getCalcType() != null && item.getCalcType() == FeeItem.CALC_TYPE_FIXED
                    && item.getFixedAmount() != null) {
                fee.setAmount(item.getFixedAmount());
                fee.setCalcBasis("固定金额 " + item.getFixedAmount() + " 元");
            } else {
                continue;
            }
            fee.setCreateBy(operatorId);
            fee.setCreateTime(now);
            fee.setUpdateBy(operatorId);
            fee.setUpdateTime(now);
            this.save(fee);
            created.add(fee);
        }
        log.info("[附加费] 自动生成 orderId={} 运费={} 生成 {} 项（启用 {} 项）",
                orderId, order.getAmount(), created.size(), items.size());
        return created;
    }
}
