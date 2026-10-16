package com.xinfei.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xinfei.DTO.OrderCargoDto;
import com.xinfei.DTO.OrderCancelDTO;
import com.xinfei.DTO.OrderDTO;
import com.xinfei.DTO.OrderLocationDto;
import com.xinfei.DTO.TaskPickupDispatchDTO;
import com.xinfei.DTO.TransportOrderDTO;
import com.xinfei.DTO.AreaDTO;
import com.xinfei.common.utils.PageResponse;
import com.xinfei.common.utils.Result;
import com.xinfei.enums.pickuptask.PickupDispatchTaskStatus;
import com.xinfei.enums.pickuptask.PickupDispatchTaskType;
import com.xinfei.feign.AreaFeign;
import com.xinfei.feign.CargoFeign;
import com.xinfei.feign.OrderFeign;
import com.xinfei.feign.OrgFeign;
import com.xinfei.feign.PickupDispatchTaskFeign;
import com.xinfei.feign.TransportOrderFeign;
import com.xinfei.feign.UserFeign;
import com.xinfei.util.BeanUtil;
import com.xinfei.vo.oms.OrderVo;
import com.xinfei.util.Rx;
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
    private CargoFeign cargoFeign;
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

    @Operation(summary = "运费试算（透传 xf-oms Drools 计费）")
    @PostMapping("/orderMsg")
    @SaCheckPermission("order:list")
    public Map<String, Object> getOrderMsg(@RequestBody OrderDTO dto) {
        // xf-oms 契约：orderCargoDto 为空或 totalWeight<=0 时返回空 Map；地址解析失败返回 amount="0"+errorMsg；参数不全返回 null
        return orderFeign.getOrderMsg(dto);
    }

    @Operation(summary = "新建订单（透传 xf-oms，含货物明细；save 内已保存货物，勿重复调 cargo save）")
    @PostMapping("/create")
    @SaCheckPermission("order:create")
    public OrderDTO create(@RequestBody OrderDTO dto) {
        // 失败（货物缺失/地址哨兵）时远端返回 null，由全局响应壳处理
        return orderFeign.save(dto);
    }

    @Operation(summary = "获取订单分页数据")
    @PostMapping("/page")
    @SaCheckPermission("order:list")
    public PageResponse<OrderVo> findByPage(@RequestBody OrderVo vo) {
        PageResponse<OrderDTO> orderPage = orderFeign.findByPage(BeanUtil.parseOrderVo2DTO(vo));
        //加工数据
        // 分页 items 可能为 null，统一通过 Rx 安全取值
        List<OrderDTO> orderDTOList = Rx.items(orderPage);
        // 区划批量预置：一页订单的发/收省市区去重后一次远程查，避免逐单 6 次（分页 20 单 = 120 次）
        Map<Long, AreaDTO> areasById = BeanUtil.loadAreasBatch(orderDTOList, areaFeign);
        List<OrderVo> orderVoList = orderDTOList.stream()
                .map(orderDTO -> BeanUtil.parseOrderDTO2Vo(orderDTO, areaFeign, areasById))
                .collect(Collectors.toList());
        return PageResponse.<OrderVo>builder().items(orderVoList).pagesize(vo.getPageSize()).page(vo.getPage()).counts(orderPage != null && orderPage.getCounts() != null ? orderPage.getCounts() : 0L).pages(orderPage != null && orderPage.getPages() != null ? orderPage.getPages() : 0L).build();
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
     * <p>主动作：调 xf-oms /order/{id}/cancel 完成订单本体状态流转（1~9 → 12）并持久化取消原因/时间；
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
     * 支付确认（透传 xf-oms /{id}/pay，订单状态流转+支付时间落库）
     */
    @Operation(summary = "支付确认")
    @PutMapping("/{id}/pay")
    @SaCheckPermission("order:pay")
    public Result payOrder(@PathVariable(name = "id") String id) {
        return orderFeign.pay(id);
    }

    /**
     * 改价（FR-75 决策留痕 + 服务端 Drools 重算运费）。
     * 入参只需改价理由；地址/货物由 BFF 从 xf-oms 详情与货物端点组装后透传重算，
     * 计算结果以服务端为准，前端不可篡改金额。reasonEnum 枚举白名单：
     * COST_CORRECTION 成本修正 / QUOTATION_ERROR 报价错误 / CUSTOMER_DISCOUNT 客户优惠。
     */
    @Operation(summary = "改价（重算运费，含决策留痕）")
    @PutMapping("/{id}/reprice")
    @SaCheckPermission("order:reprice")
    public Result repriceOrder(@PathVariable(name = "id") String id,
                               @RequestParam(value = "reasonEnum", required = false) String reasonEnum) {
        OrderDTO order = orderFeign.findById(id);
        if (order == null || order.getId() == null) {
            return Result.error(404, "订单不存在");
        }
        List<OrderCargoDto> cargos = cargoFeign.findAll(null, id);
        if (cargos == null || cargos.isEmpty()) {
            return Result.error(400, "订单缺少货物明细，无法重算运费");
        }
        // 只组装计费所需字段：完整透传会把 createTime 等 "yyyy-MM-dd HH:mm:ss" 字符串
        // 再次发往 xf-oms，LocalDateTime 反序列化直接 400（实测踩坑）；距离由服务端重算
        OrderDTO repriceDto = new OrderDTO();
        repriceDto.setId(id);
        repriceDto.setSenderAddress(order.getSenderAddress());
        repriceDto.setSenderProvinceId(order.getSenderProvinceId());
        repriceDto.setSenderCityId(order.getSenderCityId());
        repriceDto.setSenderCountyId(order.getSenderCountyId());
        repriceDto.setReceiverAddress(order.getReceiverAddress());
        repriceDto.setReceiverProvinceId(order.getReceiverProvinceId());
        repriceDto.setReceiverCityId(order.getReceiverCityId());
        repriceDto.setReceiverCountyId(order.getReceiverCountyId());
        repriceDto.setOrderCargoDto(cargos.get(0));
        return orderFeign.reprice(id, repriceDto, reasonEnum);
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
