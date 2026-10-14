/**
 * Copyright (c) 2019 联智合创 All rights reserved.
 * <p>
 * http://www.witlinked.com
 * <p>
 * 版权所有，侵权必究！
 */

package com.xinfei.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;

import com.xinfei.DTO.TaskPickupDispatchDTO;
import com.xinfei.DTO.TaskTransportDTO;
import com.xinfei.DTO.TransportOrderDTO;
import com.xinfei.enums.pickuptask.PickupDispatchTaskType;
import com.xinfei.feign.PickupDispatchTaskFeign;
import com.xinfei.feign.TransportOrderFeign;
import com.xinfei.feign.TransportTaskFeign;
import com.xinfei.vo.oms.OrderLocusVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单轨迹
 *
 * @author
 */
@RestController
@RequestMapping("/orderLocus")
@Tag(name = "订单轨迹")
@Slf4j
public class OrderLocusController {

    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private TransportOrderFeign transportOrderFeign;

    @Autowired
    private TransportTaskFeign transportTaskFeign;

    @Autowired
    private PickupDispatchTaskFeign pickupDispatchTaskFeign;

    @GetMapping("{id}")
    @SaCheckPermission("order:detail")
    @Operation(summary = "查询订单轨迹参数")
    public List<OrderLocusVo> findLocusByOrderId(@PathVariable("id") String id) {
        List<OrderLocusVo> orderLocusVos = new ArrayList<>();
        // 根据订单号查询快递员取件任务
        TaskPickupDispatchDTO pickupDispatchTaskDto = pickupDispatchTaskFeign.findByOrderId(Long.valueOf(id), PickupDispatchTaskType.PICKUP.getCode());
        log.info("订单轨迹-取件任务：{}", pickupDispatchTaskDto);
        if (pickupDispatchTaskDto != null) {
            OrderLocusVo orderLocusVo = new OrderLocusVo();

            if (pickupDispatchTaskDto.getCourierId() != null) {
                orderLocusVo.setBusinessId(String.valueOf(pickupDispatchTaskDto.getCourierId()));
            }
            orderLocusVo.setGe___time(dtf.format(pickupDispatchTaskDto.getCreateTime())); // 起始时间设置成任务创建时间
            if (pickupDispatchTaskDto.getActualEndTime() == null) {
                orderLocusVo.setLe___time(dtf.format(LocalDateTime.now()));
            } else {
                orderLocusVo.setLe___time(dtf.format(pickupDispatchTaskDto.getActualEndTime()));
            }
            orderLocusVos.add(orderLocusVo);
        }


        // 根据订单号 查询运单信息
        TransportOrderDTO transportOrderDto = transportOrderFeign.findByOrderId(Long.valueOf(id));
        log.info("订单轨迹-运输任务：{}", transportOrderDto);
        if (transportOrderDto != null) {
            List<TaskTransportDTO> transportTaskDto = transportTaskFeign.findAllByOrderIdOrTaskId(transportOrderDto.getId(), null);

            if (transportTaskDto != null) {
                for (TaskTransportDTO taskTransportDTO : transportTaskDto) {
                    OrderLocusVo orderLocusVo = new OrderLocusVo();
                    orderLocusVo.setTransportTaskId(String.valueOf(taskTransportDTO.getId()));
                    orderLocusVos.add(orderLocusVo);
                }
            }
        }

        // 根据订单号查询快递员派件任务
        TaskPickupDispatchDTO pickupDispatchTaskPushDto = pickupDispatchTaskFeign.findByOrderId(Long.valueOf(id), PickupDispatchTaskType.DISPATCH.getCode());
        log.info("订单轨迹-派件任务：{}", pickupDispatchTaskPushDto);
        if (pickupDispatchTaskPushDto != null) {
            OrderLocusVo orderLocusVo = new OrderLocusVo();

            if (pickupDispatchTaskPushDto.getCourierId() != null) {
                orderLocusVo.setBusinessId(String.valueOf(pickupDispatchTaskPushDto.getCourierId()));
            }
            orderLocusVo.setGe___time(dtf.format(pickupDispatchTaskPushDto.getCreateTime())); // 起始时间设置成任务创建时间
            if (pickupDispatchTaskPushDto.getActualEndTime() == null) {
                orderLocusVo.setLe___time(dtf.format(LocalDateTime.now()));
            } else {
                orderLocusVo.setLe___time(dtf.format(pickupDispatchTaskPushDto.getActualEndTime()));
            }
            orderLocusVos.add(orderLocusVo);
        }

        log.info("订单轨迹-轨迹参数：{}", orderLocusVos);
        return orderLocusVos;
    }
}
