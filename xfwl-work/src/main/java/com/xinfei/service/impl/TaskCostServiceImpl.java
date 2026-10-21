package com.xinfei.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xinfei.entity.TaskCost;
import com.xinfei.mapper.TaskCostMapper;
import com.xinfei.service.ITaskCostService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 趟次成本 Service 实现（P0-6d/OPS-4）
 */
@Slf4j
@Service
public class TaskCostServiceImpl extends ServiceImpl<TaskCostMapper, TaskCost>
        implements ITaskCostService {

    private static final Set<String> VALID_TYPES = Set.of(
            TaskCost.TYPE_FUEL, TaskCost.TYPE_TOLL, TaskCost.TYPE_REPAIR,
            TaskCost.TYPE_FINE, TaskCost.TYPE_LABOR, TaskCost.TYPE_OTHER);

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskCost saveCost(TaskCost cost) {
        if (cost == null || cost.getTaskTransportId() == null || cost.getAmount() == null
                || cost.getOccurTime() == null || cost.getCostType() == null) {
            throw new IllegalArgumentException("趟次id/成本类型/金额/发生时间必填");
        }
        if (!VALID_TYPES.contains(cost.getCostType())) {
            throw new IllegalArgumentException("成本类型非法: " + cost.getCostType()
                    + "（允许 FUEL/TOLL/REPAIR/FINE/LABOR/OTHER）");
        }
        if (cost.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("金额不能为负");
        }
        LocalDateTime now = LocalDateTime.now();
        cost.setCreateTime(now);
        cost.setUpdateTime(now);
        baseMapper.insert(cost);
        log.info("[成本] 录入成功: taskId={}, type={}, amount={}",
                cost.getTaskTransportId(), cost.getCostType(), cost.getAmount());
        return cost;
    }

    @Override
    public Map<String, Object> taskCostDetail(Long taskTransportId) {
        List<TaskCost> items = baseMapper.selectList(new LambdaQueryWrapper<TaskCost>()
                .eq(TaskCost::getTaskTransportId, taskTransportId)
                .orderByDesc(TaskCost::getOccurTime));
        BigDecimal total = items.stream()
                .map(c -> c.getAmount() == null ? BigDecimal.ZERO : c.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Object> vo = new HashMap<>();
        vo.put("items", items);
        vo.put("total", total);
        return vo;
    }

    @Override
    public Page<TaskCost> page(Integer page, Integer pageSize, Long taskTransportId, Long truckId, String costType) {
        Page<TaskCost> p = new Page<>(page == null || page < 1 ? 1 : page,
                pageSize == null || pageSize < 1 ? 10 : pageSize);
        LambdaQueryWrapper<TaskCost> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(taskTransportId != null, TaskCost::getTaskTransportId, taskTransportId)
                .eq(truckId != null, TaskCost::getTruckId, truckId)
                .eq(costType != null && !costType.isBlank(), TaskCost::getCostType, costType)
                .orderByDesc(TaskCost::getOccurTime);
        return baseMapper.selectPage(p, wrapper);
    }
}
