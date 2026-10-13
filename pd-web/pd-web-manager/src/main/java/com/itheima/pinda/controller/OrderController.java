package com.itheima.pinda.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.itheima.pinda.DTO.OrderCancelDTO;
import com.itheima.pinda.DTO.OrderDTO;
import com.itheima.pinda.DTO.OrderLocationDto;
import com.itheima.pinda.DTO.TaskPickupDispatchDTO;
import com.itheima.pinda.DTO.TransportOrderDTO;
import com.itheima.pinda.common.utils.PageResponse;
import com.itheima.pinda.common.utils.Result;
import com.itheima.pinda.enums.pickuptask.PickupDispatchTaskStatus;
import com.itheima.pinda.enums.pickuptask.PickupDispatchTaskType;
import com.itheima.pinda.feign.AreaFeign;
import com.itheima.pinda.feign.OrderFeign;
import com.itheima.pinda.feign.OrgFeign;
import com.itheima.pinda.feign.PickupDispatchTaskFeign;
import com.itheima.pinda.feign.TransportOrderFeign;
import com.itheima.pinda.feign.UserFeign;
import com.itheima.pinda.util.BeanUtil;
import com.itheima.pinda.vo.oms.OrderVo;
import com.itheima.pinda.util.Rx;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 订单状态  前端控制器
 * </p>
 *
 * @author jpf
 * @since 2019-12-26
 */
@Slf4j
@RestController
@Tag(name = "订单相关API")
@RequestMapping("order-manager/order")
public class OrderController {
    @Autowired
    private OrderFeign orderFeign;
    @Autowired
    private PickupDispatchTaskFeign pickupDispatchTaskFeign;
    @Autowired
    private AreaFeign areaFeign;
    @Autowired
    private OrgFeign orgFeign;
    @Autowired
    private UserFeign userFeign;
    @Autowired
    private TransportOrderFeign transportOrderFeign;

    @Operation(summary = "获取订单分页数据")
    @PostMapping("/page")
    @SaCheckPermission("order:list")
    public PageResponse<OrderVo> findByPage(@RequestBody OrderVo vo) {
        PageResponse<OrderDTO> orderPage = orderFeign.findByPage(BeanUtil.parseOrderVo2DTO(vo));
        //加工数据
        // 分页 items 可能为 null，统一通过 Rx 安全取值
        List<OrderDTO> orderDTOList = Rx.items(orderPage);
        List<OrderVo> orderVoList = orderDTOList.stream().map(orderDTO -> BeanUtil.parseOrderDTO2Vo(orderDTO, areaFeign)).collect(Collectors.toList());
        return PageResponse.<OrderVo>builder().items(orderVoList).pagesize(vo.getPageSize()).page(vo.getPage()).counts(orderPage.getCounts()).pages(orderPage.getPages()).build();
    }

    @Operation(summary = "获取订单详情")
    @GetMapping("/{id}")
    @SaCheckPermission("order:detail")
    public OrderVo findOrderById(@PathVariable(name = "id") String id) {
        // 远程查询可能返回 null，避免后续 NPE
        OrderDTO orderDTO = orderFeign.findById(id);
        if (orderDTO == null) {
            return new OrderVo();
        }
        OrderVo vo = BeanUtil.parseOrderDTO2Vo(orderDTO, areaFeign);
        if (StringUtils.isNotEmpty(vo.getId())) {
            //查询取派件任务信息
            TaskPickupDispatchDTO taskPickupDispatchQueryDTO = new TaskPickupDispatchDTO();
            taskPickupDispatchQueryDTO.setOrderId(Long.valueOf(vo.getId()));
            List<TaskPickupDispatchDTO> taskPickupDispatchDTOList = pickupDispatchTaskFeign.findAll(taskPickupDispatchQueryDTO);
            if (taskPickupDispatchDTOList != null && taskPickupDispatchDTOList.size() > 0) {
                taskPickupDispatchDTOList.forEach(taskPickupDispatchDTO -> {
                    if (String.valueOf(taskPickupDispatchDTO.getOrderId()).equals(vo.getId()) && taskPickupDispatchDTO.getTaskType() == PickupDispatchTaskType.PICKUP.getCode()) {
                        //取件信息
                        vo.setTaskPickup(BeanUtil.parseTaskPickupDispatchDTO2Vo(taskPickupDispatchDTO, orderFeign, areaFeign, orgFeign, userFeign));
                    }
                    if (String.valueOf(taskPickupDispatchDTO.getOrderId()).equals(vo.getId()) && taskPickupDispatchDTO.getTaskType() == PickupDispatchTaskType.DISPATCH.getCode()) {
                        //派件信息
                        vo.setTaskDispatch(BeanUtil.parseTaskPickupDispatchDTO2Vo(taskPickupDispatchDTO, orderFeign, areaFeign, orgFeign, userFeign));
                    }
                });
            }
            //查询运单信息
            TransportOrderDTO transportOrderDTO = transportOrderFeign.findByOrderId(Long.valueOf(vo.getId()));
            if (transportOrderDTO != null) {
                vo.setTransportOrder(BeanUtil.parseTransportOrderDTO2Vo(transportOrderDTO, null, null));
            }
        }
        return vo;
    }

    @Operation(summary = "更新订单")
    @PostMapping("/{id}")
    @SaCheckPermission("order:update")
    public OrderVo updateOrder(@PathVariable(name = "id") String id, @RequestBody OrderVo vo) {
        OrderDTO dto = BeanUtil.parseOrderVo2DTO(vo);
        // 金额/支付状态已由通用更新端点屏蔽，改由专用端点管理（/{id}/pay 支付确认、/{id}/reprice 重算运费）。
        // 编辑表单回显时会携带这两个字段，这里置空并继续更新其余字段，避免整个编辑被静默丢弃；
        // 如需修改金额/支付状态请走对应专用端点。
        if (vo.getAmount() != null || vo.getPaymentStatus() != null) {
            log.info("[订单] 管理端编辑订单忽略金额/支付状态字段(走专用端点): id={}", id);
            dto.setAmount(null);
            dto.setPaymentStatus(null);
        }
        OrderDTO updated = orderFeign.updateById(id, dto);
        // 状态流转校验失败时远端返回 null，避免 parseOrderDTO2Vo 对 null 处理引发 NPE
        if (updated == null) {
            return null;
        }
        return BeanUtil.parseOrderDTO2Vo(updated, null);
    }

    /**
     * 取消订单（BFF 编排）
     *
     * <p>主动作：调 pd-oms /order/{id}/cancel 完成订单本体状态流转（1~9 → 12）并持久化取消原因/时间；
     * 主动作失败时原样透传远端 Result（含 code/msg），前端拦截器统一提示，不执行任何联动。</p>
     *
     * <p>主动作成功后编排下游（无分布式事务，任一步失败只收集进 warnings，不回滚订单取消）：
     * 删除订单位置；取件/派件任务仅在 1待执行/2进行中/3待确认 时置 5已取消（4已完成不动）；
     * 运单无取消态且干线一车多单，不自动改状态，存在关联运单时收集 warning 交人工处理。</p>
     *
     * @param id        订单id
     * @param cancelDTO 取消入参（reason 必填）
     * @return 成功返回 {warnings: [...]}（warnings 为空数组表示全部联动成功）；失败透传远端 Result
     */
    @Operation(summary = "取消订单（原因持久化+取派件任务联动）")
    @PutMapping("/{id}/cancel")
    @SaCheckPermission("order:cancel")
    public Map<String, Object> cancelOrder(@PathVariable(name = "id") String id, @RequestBody OrderCancelDTO cancelDTO) {
        // 主动作：订单本体取消。Result 是含 "code" 的 Map，GlobalResponseAdvice 会原样透传
        Result cancelResult = orderFeign.cancel(id, cancelDTO);
        if (cancelResult == null || !Integer.valueOf(0).equals(cancelResult.get("code"))) {
            log.warn("[订单取消] 订单本体取消失败，终止下游联动: id={}, result={}", id, cancelResult);
            return cancelResult;
        }

        Long orderId = Long.valueOf(id);
        String reason = cancelDTO.getReason() == null ? "" : cancelDTO.getReason().trim();
        List<String> warnings = new ArrayList<>();

        // 1. 删除订单位置（与客户端 MailingController#cancel 同范式），失败不影响取消结果
        try {
            OrderLocationDto locationDto = new OrderLocationDto();
            locationDto.setOrderId(id);
            orderFeign.deleteOrderLocation(locationDto);
        } catch (Exception e) {
            log.warn("[订单取消] 订单位置清理失败: id={}", id, e);
            warnings.add("订单位置清理失败，不影响取消结果，可稍后人工核实");
        }

        // 2. 取件任务、派件任务联动取消
        cancelDownstreamTask(orderId, PickupDispatchTaskType.PICKUP.getCode(), "取件", reason, warnings);
        cancelDownstreamTask(orderId, PickupDispatchTaskType.DISPATCH.getCode(), "派件", reason, warnings);

        // 3. 运单无取消态，且干线运输任务一车多单，禁止自动联动，仅收集提示交人工处理
        try {
            TransportOrderDTO transportOrder = transportOrderFeign.findByOrderId(orderId);
            if (transportOrder != null && transportOrder.getId() != null) {
                String tip = Integer.valueOf(3).equals(transportOrder.getSchedulingStatus())
                        ? "该订单存在已调度关联运单，干线运输任务需人工核实处理"
                        : "该订单存在关联运单，运单状态需人工核实处理";
                warnings.add(tip);
            }
        } catch (Exception e) {
            log.warn("[订单取消] 关联运单查询失败: id={}", id, e);
            warnings.add("关联运单查询失败，请人工核实运单与干线任务状态");
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("warnings", warnings);
        return resp;
    }

    /**
     * 联动取消单个取/派件任务：任务存在且状态为 1待执行/2进行中/3待确认 时置 5已取消；
     * 4已完成、5已取消 或不存在时跳过。任何异常只收集 warning，不上抛。
     */
    private void cancelDownstreamTask(Long orderId, Integer taskType, String taskName, String reason, List<String> warnings) {
        try {
            TaskPickupDispatchDTO task = pickupDispatchTaskFeign.findByOrderId(orderId, taskType);
            if (task == null || task.getId() == null) {
                return;
            }
            Integer status = task.getStatus();
            // 仅联动未终态任务；4已完成不可改、5已取消幂等跳过
            Set<Integer> cancellableStatuses = Set.of(
                    PickupDispatchTaskStatus.PENDING.getCode(),
                    PickupDispatchTaskStatus.PROCESSING.getCode(),
                    PickupDispatchTaskStatus.CONFIRM.getCode());
            if (status == null || !cancellableStatuses.contains(status)) {
                return;
            }
            TaskPickupDispatchDTO updateDto = new TaskPickupDispatchDTO();
            updateDto.setStatus(PickupDispatchTaskStatus.CANCELLED.getCode());
            updateDto.setCancelTime(LocalDateTime.now());
            // mark 列为 VARCHAR(50)，完整取消原因已持久化到 oms_order.cancel_reason，此处截断备注
            String briefReason = reason.length() > 40 ? reason.substring(0, 40) + "…" : reason;
            updateDto.setMark("管理端取消：" + briefReason);
            pickupDispatchTaskFeign.updateById(task.getId(), updateDto);
        } catch (Exception e) {
            log.warn("[订单取消] {}任务联动取消失败: orderId={}", taskName, orderId, e);
            warnings.add(taskName + "任务联动取消失败，当前状态为「" + taskName + "中」时需人工取消");
        }
    }
}
