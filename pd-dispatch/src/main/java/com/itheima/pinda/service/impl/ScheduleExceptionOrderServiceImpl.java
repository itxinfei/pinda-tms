package com.itheima.pinda.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.pinda.entity.ScheduleExceptionOrder;
import com.itheima.pinda.mapper.ScheduleExceptionOrderMapper;
import com.itheima.pinda.service.IScheduleExceptionOrderService;
import com.itheima.pinda.utils.IdUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 异常调度订单 Service 实现
 */
@Slf4j
@Service
public class ScheduleExceptionOrderServiceImpl extends ServiceImpl<ScheduleExceptionOrderMapper, ScheduleExceptionOrder>
        implements IScheduleExceptionOrderService {

    /**
     * 登记无法调度的订单（幂等：同一订单存在待处理记录时不重复登记）
     *
     * @param orderId  订单ID
     * @param agencyId 当前机构ID
     * @param reason   异常原因
     * @return 是否新增登记
     */
    @Override
    public boolean registerExceptionOrder(String orderId, String agencyId, String reason) {
        if (StringUtils.isBlank(orderId)) {
            return false;
        }
        // 幂等：存在待处理的同订单记录则跳过
        LambdaQueryWrapper<ScheduleExceptionOrder> existsWrapper = new LambdaQueryWrapper<>();
        existsWrapper.eq(ScheduleExceptionOrder::getOrderId, orderId)
            .eq(ScheduleExceptionOrder::getStatus, ScheduleExceptionOrder.STATUS_PENDING);
        long exists = count(existsWrapper);
        if (exists > 0) {
            log.info("[异常调度] 订单[{}]已有待处理异常登记，跳过重复登记", orderId);
            return false;
        }

        ScheduleExceptionOrder record = new ScheduleExceptionOrder();
        record.setId(IdUtils.get());
        record.setOrderId(orderId);
        record.setAgencyId(agencyId);
        record.setReason(StringUtils.defaultIfBlank(reason, "起始/目的机构信息缺失，无法完成线路规划"));
        record.setStatus(ScheduleExceptionOrder.STATUS_PENDING);
        record.setCreateTime(LocalDateTime.now());
        try {
            save(record);
            log.info("[异常调度] 订单[{}]登记为无法调度，原因: {}", orderId, record.getReason());
            return true;
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // 唯一索引(uk_order_status)兜底：并发登记时视为幂等成功
            log.info("[异常调度] 订单[{}]并发登记被唯一索引拦截，视为已登记", orderId);
            return false;
        }
    }
}
