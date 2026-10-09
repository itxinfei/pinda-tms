package com.itheima.pinda.controller;

import com.itheima.pinda.DTO.OrderDTO;
import com.itheima.pinda.DTO.TaskTransportDTO;
import com.itheima.pinda.DTO.TransportOrderDTO;
import com.itheima.pinda.common.context.RequestContext;
import com.itheima.pinda.common.utils.OwnershipAssert;
import com.itheima.pinda.common.utils.Result;
import com.itheima.pinda.feign.OrderFeign;
import com.itheima.pinda.feign.TraceFeign;
import com.itheima.pinda.feign.TransportOrderFeign;
import com.itheima.pinda.feign.TransportTaskFeign;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 客户订单轨迹查询。
 *
 * <p>把"我的订单"串到在途轨迹：订单 → 运单 → 运输任务 → pd_netty.pd_truck_location 轨迹点。
 * 对外路径 {@code /api/web-customer/orderTrace/trace}。全程先做 memberId 归属校验，
 * 任何一环缺失都返回空轨迹，不报错，保证小程序在下单后、调度前也能正常展示状态。</p>
 */
@Slf4j
@Api(tags = "客户订单轨迹")
@RestController
@RequestMapping("orderTrace")
public class OrderTraceController {

    private final OrderFeign orderFeign;
    private final TransportOrderFeign transportOrderFeign;
    private final TransportTaskFeign transportTaskFeign;
    private final TraceFeign traceFeign;

    public OrderTraceController(OrderFeign orderFeign,
                                TransportOrderFeign transportOrderFeign,
                                TransportTaskFeign transportTaskFeign,
                                TraceFeign traceFeign) {
        this.orderFeign = orderFeign;
        this.transportOrderFeign = transportOrderFeign;
        this.transportTaskFeign = transportTaskFeign;
        this.traceFeign = traceFeign;
    }

    @ApiOperation("按订单查轨迹")
    @GetMapping("trace")
    public Result trace(String orderId) {
        if (StringUtils.isBlank(orderId)) {
            return Result.error(400, "orderId不能为空");
        }
        String userId = RequestContext.getUserId();
        OrderDTO order = orderFeign.findById(orderId);
        if (order == null) {
            return Result.error(400, "订单不存在");
        }
        // 归属校验：只允许查本人订单，防止水平越权
        Result deny = OwnershipAssert.checkEquals(order.getMemberId(), userId, "无权查看他人订单");
        if (deny != null) {
            return deny;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("orderId", orderId);
        data.put("orderStatus", order.getStatus());

        // 运单可能尚未生成（下单后、调度前），此时轨迹为空属正常
        List<TaskTransportDTO> tasks = Collections.emptyList();
        TransportOrderDTO transportOrder = transportOrderFeign.findByOrderId(orderId);
        if (transportOrder != null) {
            data.put("transportOrderId", transportOrder.getId());
            data.put("transportOrderStatus", transportOrder.getStatus());
            List<TaskTransportDTO> queried =
                    transportTaskFeign.findAllByOrderIdOrTaskId(transportOrder.getId(), null);
            if (queried != null) {
                tasks = queried;
            }
        } else {
            data.put("transportOrderId", null);
            data.put("transportOrderStatus", null);
        }

        List<String> taskIds = tasks.stream()
                .map(TaskTransportDTO::getId)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());

        Object tracks = Collections.emptyList();
        if (!taskIds.isEmpty()) {
            Result traceResult = traceFeign.byTasks(taskIds);
            if (traceResult != null && traceResult.get("data") != null) {
                tracks = traceResult.get("data");
            }
        }
        data.put("tracks", tracks);
        log.info("[订单轨迹] orderId={}, 运输任务数={}, 轨迹点已返回", orderId, taskIds.size());
        return Result.ok().put("data", data);
    }
}
