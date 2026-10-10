package com.itheima.pinda.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.itheima.pinda.common.utils.IdConverter;
import com.itheima.pinda.entity.OrderCargo;
import com.itheima.pinda.mapper.OrderCargoMapper;
import com.itheima.pinda.service.IOrderCargoService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 货物服务实现类
 */
@Service
public class OrderCargoServiceImpl extends ServiceImpl<OrderCargoMapper, OrderCargo> implements IOrderCargoService {

    /**
     * 保存货物：有id走更新，无id走新增（主键 ASSIGN_ID 雪花自动赋值）。
     */
    @Override
    public OrderCargo saveSelective(OrderCargo record) {
        if (record.getId() != null) {
            this.updateById(record);
        } else {
            this.save(record);
        }
        return record;
    }

    /**
     * 按运单/订单查询货物。对外参数名保留 tranOrderId（共享契约），
     * 内部转换为 transportOrderId。
     */
    @Override
    public List<OrderCargo> findAll(String tranOrderId, String orderId) {
        LambdaQueryWrapper<OrderCargo> queryWrapper = new LambdaQueryWrapper<>();
        Long transportOrderId = IdConverter.toLong(tranOrderId);
        Long orderIdValue = IdConverter.toLong(orderId);
        if (transportOrderId != null) {
            queryWrapper.eq(OrderCargo::getTransportOrderId, transportOrderId);
        }
        if (orderIdValue != null) {
            queryWrapper.eq(OrderCargo::getOrderId, orderIdValue);
        }
        queryWrapper.orderBy(true, true, OrderCargo::getId);
        return baseMapper.selectList(queryWrapper);
    }
}
