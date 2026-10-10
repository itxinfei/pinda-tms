package com.itheima.pinda.controller;


import com.itheima.pinda.DTO.*;
import com.itheima.pinda.DTO.base.GoodsTypeDto;
import com.itheima.pinda.common.context.RequestContext;
import com.itheima.pinda.common.exception.PdException;
import com.itheima.pinda.common.utils.IdCardUtils;
import com.itheima.pinda.common.utils.OwnershipAssert;
import com.itheima.pinda.common.utils.PageResponse;
import com.itheima.pinda.common.utils.Result;
import com.itheima.pinda.enums.MemberIdCardVerifyStatus;
import com.itheima.pinda.enums.OrderStatus;
import com.itheima.pinda.enums.OrderType;
import com.itheima.pinda.enums.pickuptask.PickupDispatchTaskSignStatus;
import com.itheima.pinda.enums.pickuptask.PickupDispatchTaskStatus;
import com.itheima.pinda.enums.pickuptask.PickupDispatchTaskType;
import com.itheima.pinda.enums.transportorder.TransportOrderSchedulingStatus;
import com.itheima.pinda.enums.transportorder.TransportOrderStatus;
import com.itheima.pinda.event.OrderConfirmedEvent;
import com.itheima.pinda.event.OrderDeliveredEvent;
import com.itheima.pinda.event.PickupCompletedEvent;
import com.itheima.pinda.mq.EventPublisher;
import com.itheima.pinda.DTO.ExceptionReportFeignDTO;
import com.itheima.pinda.feign.ExceptionReportFeign;
import com.itheima.pinda.feign.*;
import com.itheima.pinda.feign.common.GoodsTypeFeign;
import com.itheima.pinda.feign.courier.AppCourierFeign;
import com.itheima.pinda.future.PdCompletableFuture;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * <p>
 * 运单表 前端控制器
 * </p>
 *
 * @author diesel
 * @since 2020-03-24
 */
@Slf4j
@Tag(name = "快递员业务")
@Controller
@RequestMapping("courier")
public class CourierController {

    private final PickupDispatchTaskFeign pickupDispatchTaskFeign;

    private final OrderFeign orderFeign;

    private final CargoFeign cargoFeign;

    private final AreaFeign areaFeign;

    private final GoodsTypeFeign goodsTypeFeign;

    private final TransportOrderFeign transportOrderFeign;

    private final TransportTaskFeign transportTaskFeign;

    private final OrgFeign orgFeign;

    private final MemberFeign memberFeign;

    private final AppCourierFeign appCourierFeign;

    private final EventPublisher eventPublisher;
    private final ExceptionReportFeign exceptionReportFeign;


    public CourierController(AppCourierFeign appCourierFeign, MemberFeign memberFeign, OrgFeign orgFeign, TransportTaskFeign transportTaskFeign, TransportOrderFeign transportOrderFeign, GoodsTypeFeign goodsTypeFeign, PickupDispatchTaskFeign pickupDispatchTaskFeign, OrderFeign orderFeign, CargoFeign cargoFeign, AreaFeign areaFeign, EventPublisher eventPublisher, ExceptionReportFeign exceptionReportFeign) {
        this.appCourierFeign = appCourierFeign;
        this.memberFeign = memberFeign;
        this.pickupDispatchTaskFeign = pickupDispatchTaskFeign;
        this.goodsTypeFeign = goodsTypeFeign;
        this.orderFeign = orderFeign;
        this.cargoFeign = cargoFeign;
        this.areaFeign = areaFeign;
        this.transportOrderFeign = transportOrderFeign;
        this.transportTaskFeign = transportTaskFeign;
        this.orgFeign = orgFeign;
        this.eventPublisher = eventPublisher;
        this.exceptionReportFeign = exceptionReportFeign;
    }

    @SneakyThrows
    @Operation(summary = "待取件/待妥投")
    @Parameters({
            @Parameter(name = "page", description = "当前页数", required = true, example = "1"),
            @Parameter(name = "pagesize", description = "每页条数", required = true, example = "10"),
            @Parameter(name = "taskType", description = "类型", required = true, example = ""),
            @Parameter(name = "status", description = "状态", required = true, example = ""),
            @Parameter(name = "keyword", description = "搜索条件", required = false, example = ""),
            @Parameter(name = "date", description = "时间", required = false, example = "")
            //@Parameter(name = "statusArray", description = "批量状态", required = false, example = "")
    })
    @ResponseBody
    @GetMapping("pickupDispatch")
    public Result pickupDispatch(Integer page, Integer pagesize, Integer taskType, Integer status, String keyword, String date) {

        //  快递员id  并放入参数
        String courierId = RequestContext.getUserId();

        AppCourierQueryDTO appCourierQueryDTO = new AppCourierQueryDTO();
        appCourierQueryDTO.setPage(page);
        appCourierQueryDTO.setPageSize(pagesize);
        appCourierQueryDTO.setCourierId(courierId);
        //状态在 PickupDispatchTaskStatus 中
        appCourierQueryDTO.setStatus(status);
        //类型在 PickupDispatchTaskType 中
        appCourierQueryDTO.setTaskType(taskType);
        if (StringUtils.isNotBlank(keyword)) {
            appCourierQueryDTO.setKeyword(keyword);
        }
        if (StringUtils.isNotEmpty(date)) {
            appCourierQueryDTO.setDate(date);
        }
        log.info("查询任务信息：{}", appCourierQueryDTO);
        PageResponse<TaskPickupDispatchDTO> result = appCourierFeign.findByPage(appCourierQueryDTO);
        if (result.getItems() == null || result.getItems().size() == 0) {
            return Result.ok().put("data", PageResponse.<PickupDispatchDTO>builder().page(result.getPage()).pagesize(result.getPagesize()).pages(result.getPages()).counts(result.getCounts()).build());
        }
        log.info("查询到任务信息：{}", result.getItems());
        // 构建orderId集合
        Set<Long> orderSet = result.getItems().stream().map(TaskPickupDispatchDTO::getOrderId).collect(Collectors.toSet());
        //查询订单信息
        CompletableFuture<Map<String, OrderDTO>> orderMapFuture = PdCompletableFuture.orderMapFuture(orderFeign, orderSet);
        // 查询运单信息
        CompletableFuture<Map<Long, TransportOrderDTO>> tranOrderMapFuture = PdCompletableFuture.tranOrderMapFuture(transportOrderFeign, orderSet);

        Map<String, OrderDTO> orderMap = orderMapFuture.get();
        log.info("根据任务信息获取订单数据：{}，result:{}", orderSet, orderMap);
        Collection<OrderDTO> orderDTOs = orderMap.values();

        //查询地址信息
        Set<Long> addressSet = new HashSet<>();
        addressSet.addAll(orderDTOs.stream().filter(item -> item.getReceiverProvinceId() != null).map(item -> Long.valueOf(item.getReceiverProvinceId())).collect(Collectors.toSet()));
        addressSet.addAll(orderDTOs.stream().filter(item -> item.getReceiverCityId() != null).map(item -> Long.valueOf(item.getReceiverCityId())).collect(Collectors.toSet()));
        addressSet.addAll(orderDTOs.stream().filter(item -> item.getReceiverCountyId() != null).map(item -> Long.valueOf(item.getReceiverCountyId())).collect(Collectors.toSet()));
        addressSet.addAll(orderDTOs.stream().filter(item -> item.getSenderProvinceId() != null).map(item -> Long.valueOf(item.getSenderProvinceId())).collect(Collectors.toSet()));
        addressSet.addAll(orderDTOs.stream().filter(item -> item.getSenderCityId() != null).map(item -> Long.valueOf(item.getSenderCityId())).collect(Collectors.toSet()));
        addressSet.addAll(orderDTOs.stream().filter(item -> item.getSenderCountyId() != null).map(item -> Long.valueOf(item.getSenderCountyId())).collect(Collectors.toSet()));
        CompletableFuture<Map<Long, AreaDTO>> areaMapFuture = PdCompletableFuture.areaMapFuture(areaFeign, null, addressSet);

        Map<Long, TransportOrderDTO> tranOrderMap = tranOrderMapFuture.get();
        log.info("根据任务信息获取运单数据：{}，result:{}", orderSet, tranOrderMap);
        Map<Long, AreaDTO> areaMap = areaMapFuture.get();

        List<PickupDispatchDTO> pickupDispatchDtos = result.getItems().stream().map(item -> new PickupDispatchDTO(item, tranOrderMap, orderMap, areaMap)).collect(Collectors.toList());
        return Result.ok().put("data", PageResponse.<PickupDispatchDTO>builder()
                .page(result.getPage())
                .pagesize(result.getPagesize())
                .pages(result.getPages()).counts(result.getCounts())
                .items(pickupDispatchDtos)
                .build());
    }

    @Operation(summary = "待取件/待妥投 数量")
    @Parameters({
            @Parameter(name = "taskType", description = "类型", required = true, example = ""),
            @Parameter(name = "status", description = "状态", required = true, example = ""),
            @Parameter(name = "keyword", description = "搜索条件", required = false, example = ""),
            @Parameter(name = "date", description = "时间", required = false, example = "")
//            @Parameter(name = "statusArray", description = "批量状态", required = false, example = "")
    })
    @ResponseBody
    @GetMapping("count")
    public Result count(Integer taskType, Integer status, String keyword, String date) {
        //  快递员id  并放入参数
        String courierId = RequestContext.getUserId();

        AppCourierQueryDTO appCourierQueryDTO = new AppCourierQueryDTO();
        appCourierQueryDTO.setPage(1);
        appCourierQueryDTO.setPageSize(1);
        appCourierQueryDTO.setCourierId(courierId);
        //状态在 PickupDispatchTaskStatus 中
        appCourierQueryDTO.setStatus(status);
        //类型在 PickupDispatchTaskType 中
        appCourierQueryDTO.setTaskType(taskType);
        if (StringUtils.isNotBlank(keyword)) {
            appCourierQueryDTO.setKeyword(keyword);
        }
        if (StringUtils.isNotEmpty(date)) {
            appCourierQueryDTO.setDate(date);
        }

        PageResponse<TaskPickupDispatchDTO> result = appCourierFeign.findByPage(appCourierQueryDTO);
        if (result.getItems() == null || result.getItems().size() == 0) {
            return Result.ok().put("count", 0);
        }
        log.info("查询数量 params:{} result：{}", appCourierQueryDTO, result.getCounts());
        return Result.ok().put("count", result.getCounts());
    }

    @SneakyThrows
    @Operation(summary = "详情页")
    @Parameter(name = "id", description = "主键", required = true, example = "")
    @ResponseBody
    @GetMapping("detail")
    public Result detail(String id) {
        log.info("任务详情:{}", id);
        if (StringUtils.isBlank(id)) {
            return Result.error("任务ID不能为空");
        }
        TaskPickupDispatchDTO pickupDispatchTaskDTO = pickupDispatchTaskFeign.findById(Long.valueOf(id));
        if (pickupDispatchTaskDTO == null || pickupDispatchTaskDTO.getOrderId() == null) {
            return Result.error("取派件任务不存在");
        }
        // 归属校验：取派任务必须属于当前登录快递员，防止越权查看他人任务详情
        Result deny = OwnershipAssert.checkEquals(toIdString(pickupDispatchTaskDTO.getCourierId()), RequestContext.getUserId(), "无权查看他人任务");
        if (deny != null) {
            return deny;
        }
        String orderId = String.valueOf(pickupDispatchTaskDTO.getOrderId());
        OrderDTO orderDTO = orderFeign.findById(orderId);
        if (orderDTO == null) {
            return Result.error("订单不存在");
        }
        List<OrderCargoDto> orderCargoDtos = cargoFeign.findAll(null, orderDTO.getId());
        if (orderCargoDtos == null || orderCargoDtos.isEmpty()) {
            return Result.error("货物信息不存在");
        }
        OrderCargoDto orderCargoDto = orderCargoDtos.get(0);

        TransportOrderDTO transportOrder = transportOrderFeign.findByOrderId(Long.valueOf(orderId));
        log.info("查询运单信息：{},RESULT:{}", orderId, transportOrder);

        Set<Long> addressSet = new HashSet<>();
        if (StringUtils.isNotEmpty(orderDTO.getReceiverProvinceId())) {
            addressSet.add(Long.valueOf(orderDTO.getReceiverProvinceId()));
        }
        if (StringUtils.isNotEmpty(orderDTO.getReceiverCityId())) {
            addressSet.add(Long.valueOf(orderDTO.getReceiverCityId()));
        }
        if (StringUtils.isNotEmpty(orderDTO.getReceiverCountyId())) {
            addressSet.add(Long.valueOf(orderDTO.getReceiverCountyId()));
        }
        if (StringUtils.isNotEmpty(orderDTO.getSenderProvinceId())) {
            addressSet.add(Long.valueOf(orderDTO.getSenderProvinceId()));
        }
        if (StringUtils.isNotEmpty(orderDTO.getSenderCityId())) {
            addressSet.add(Long.valueOf(orderDTO.getSenderCityId()));
        }
        if (StringUtils.isNotEmpty(orderDTO.getSenderCountyId())) {
            addressSet.add(Long.valueOf(orderDTO.getSenderCountyId()));
        }
        CompletableFuture<Map<Long, AreaDTO>> areaMapFuture = PdCompletableFuture.areaMapFuture(areaFeign, null, addressSet);
        Map<Long, AreaDTO> areaMap = areaMapFuture.get();
        log.info("查询物品类型：{}", orderCargoDto.getGoodsTypeId());
        GoodsTypeDto goodsType = null;
        if (StringUtils.isNotBlank(orderCargoDto.getGoodsTypeId())) {
            goodsType = goodsTypeFeign.fineById(orderCargoDto.getGoodsTypeId());
        }
        log.info("查询物品类型：{},RESULT:{}", orderCargoDto.getGoodsTypeId(), goodsType);

        MemberDTO member = memberFeign.detail(orderDTO.getMemberId());
        log.info("查询发件人信息：{}", member);
        return Result.ok().put("data", new PickupDispatchDetailDTO(pickupDispatchTaskDTO, orderDTO, orderCargoDto, goodsType, areaMap, transportOrder, member));
    }

    @SneakyThrows
    @Operation(summary = "揽收")
    @Parameter(name = "id", description = "主键", required = true, example = "")
    @ResponseBody
    @PutMapping("detail/{id}")
    public Result detail(@PathVariable("id") String id, @RequestBody PickupDispatchDetailDTO pickupDispatchDetailDTO) {
        log.info("揽收：{},{}", id, pickupDispatchDetailDTO);
        pickupDispatchDetailDTO.getGoodsTypeId();
        // 揽收时无论是否填写支付方式，订单状态都应更新为"已取件"(23001)
        // 否则交件(warehousing)时订单仍停留在"待取件"(23000)，会因状态跳变被订单状态机拦截
        OrderDTO orderEditDTO = new OrderDTO();
        orderEditDTO.setId(pickupDispatchDetailDTO.getOrderNumber());
        orderEditDTO.setStatus(OrderStatus.PICKED_UP.getCode());
        if (null != pickupDispatchDetailDTO.getPaymentMethod()) {
            orderEditDTO.setPaymentMethod(pickupDispatchDetailDTO.getPaymentMethod());
        }
        OrderDTO orderUpdateResult = orderFeign.updateById(orderEditDTO.getId(), orderEditDTO);
        if (orderUpdateResult == null) {
            log.error("揽收失败：订单状态更新未生效 orderId={}", orderEditDTO.getId());
            throw new PdException("订单状态更新失败", 500);
        }

        List<OrderCargoDto> orderCargoDtos = cargoFeign.findAll(null, pickupDispatchDetailDTO.getOrderNumber());
        log.info("揽收-订单附属信息：{},{}", pickupDispatchDetailDTO.getOrderNumber(), orderCargoDtos);
        OrderCargoDto orderCargoDto = orderCargoDtos.get(0);
        if (StringUtils.isNotEmpty(pickupDispatchDetailDTO.getGoodsTypeId())) {
            orderCargoDto.setGoodsTypeId(pickupDispatchDetailDTO.getGoodsTypeId());
        }
        if (StringUtils.isNotEmpty(pickupDispatchDetailDTO.getSustenance())) {
            orderCargoDto.setName(pickupDispatchDetailDTO.getSustenance());
        }
        if (pickupDispatchDetailDTO.getVolume() != null) {
            orderCargoDto.setVolume(pickupDispatchDetailDTO.getVolume());
            orderCargoDto.setTotalVolume(orderCargoDto.getVolume().multiply(new BigDecimal(orderCargoDto.getQuantity())));
        }
        if (pickupDispatchDetailDTO.getWeight() != null) {
            orderCargoDto.setWeight(pickupDispatchDetailDTO.getWeight());
            orderCargoDto.setTotalWeight(orderCargoDto.getWeight().multiply(new BigDecimal(orderCargoDto.getQuantity())));
        }

        log.info("揽收-修改物品附属信息:{},{}", orderCargoDto.getId(), orderCargoDto);
        OrderCargoDto cargoUpdateResult = cargoFeign.update(orderCargoDto.getId(), orderCargoDto);
        if (cargoUpdateResult == null) {
            log.error("揽收失败：货物附属信息更新未生效 cargoId={}", orderCargoDto.getId());
            throw new PdException("货物附属信息更新失败", 500);
        }
        TaskPickupDispatchDTO taskPickupDispatchDTO = new TaskPickupDispatchDTO();
        taskPickupDispatchDTO.setStatus(PickupDispatchTaskStatus.CONFIRM.getCode());
        taskPickupDispatchDTO.setActualStartTime(LocalDateTime.now());
        TaskPickupDispatchDTO taskUpdateResult = pickupDispatchTaskFeign.updateById(Long.valueOf(id), taskPickupDispatchDTO);
        if (taskUpdateResult == null) {
            log.error("揽收失败：取派件任务更新未生效 taskId={}", id);
            throw new PdException("取派件任务更新失败", 500);
        }
        log.info("更新取派件任务 ID:{},PARAMS:{}", id, taskPickupDispatchDTO);

        // 【P0优化】揽收时更新运单状态
        // 由于下单时已预生成运单，这里应该一定能查询到
        // 如果运单已存在，更新状态为"已装车"；如果不存在（异常情况），则创建
        TransportOrderDTO transportOrderDTO = transportOrderFeign.findByOrderId(Long.valueOf(pickupDispatchDetailDTO.getOrderNumber()));
        log.info("查询运单:{}", transportOrderDTO);
        if (transportOrderDTO == null || transportOrderDTO.getId() == null) {
            // 异常情况：运单不存在（理论上不应该发生，因为下单时已预生成）
            log.warn("订单[{}]未找到运单，创建新运单", pickupDispatchDetailDTO.getOrderNumber());
            TransportOrderDTO transportDTO = new TransportOrderDTO();
            transportDTO.setOrderId(Long.valueOf(pickupDispatchDetailDTO.getOrderNumber()));
            transportDTO.setStatus(TransportOrderStatus.CREATED.getCode());
            transportDTO.setSchedulingStatus(TransportOrderSchedulingStatus.TO_BE_SCHEDULED.getCode());
            TransportOrderDTO savedTransportOrder = transportOrderFeign.save(transportDTO);
            if (savedTransportOrder == null) {
                log.error("揽收失败：运单创建未生效 orderId={}", pickupDispatchDetailDTO.getOrderNumber());
                throw new PdException("运单创建失败", 500);
            }
            log.info("创建新运单:{}", transportDTO);
        } else {
            // 正常情况：更新运单状态为"已装车"
            TransportOrderDTO transportOrderUpdate = new TransportOrderDTO();
            transportOrderUpdate.setId(transportOrderDTO.getId());
            transportOrderUpdate.setStatus(TransportOrderStatus.LOADED.getCode()); // 2-已装车
            TransportOrderDTO tranOrderUpdateResult = transportOrderFeign.updateById(transportOrderDTO.getId(), transportOrderUpdate);
            if (tranOrderUpdateResult == null) {
                log.error("揽收失败：运单状态更新未生效 transportOrderId={}", transportOrderDTO.getId());
                throw new PdException("运单状态更新失败", 500);
            }
            log.info("订单[{}]已揽收，运单[{}]状态更新为[已装车(2)]",
                pickupDispatchDetailDTO.getOrderNumber(), transportOrderDTO.getId());
        }

        // 【P1优化】发布揽收完成事件（异步处理）
        // 触发后续业务逻辑：智能调度、消息通知等
        try {
            PickupCompletedEvent event = new PickupCompletedEvent(
                pickupDispatchDetailDTO.getOrderNumber(),
                toIdString(transportOrderDTO != null ? transportOrderDTO.getId() : null),
                RequestContext.getUserId(),
                id,
                null
            );
            eventPublisher.publishPickupCompleted(event);
            log.info("[事件发布] 揽收完成事件发布成功: orderId={}", pickupDispatchDetailDTO.getOrderNumber());
        } catch (Exception e) {
            log.error("[事件发布] 揽收完成事件发布失败: orderId=" + pickupDispatchDetailDTO.getOrderNumber(), e);
            // 事件发布失败不影响主流程
        }

        return Result.ok();
    }


    @SneakyThrows
    @Operation(summary = "交件")
    @Parameter(name = "tranOrderId", description = "运单号", required = true, example = "")
    @ResponseBody
    @PutMapping("warehousing/{tranOrderId}")
    public Result warehousing(@PathVariable("tranOrderId") String tranOrderId) {
        log.info(" 交件扫描运单号 ：{}", tranOrderId);

        TransportOrderDTO transportOrderDto = transportOrderFeign.findById(Long.valueOf(tranOrderId));
        if (ObjectUtils.isEmpty(transportOrderDto)) {
            return Result.error(400, "运单号未找到");
        }
        log.info(" 交件运单 ：{}", transportOrderDto);
        OrderDTO orderEditDTO = new OrderDTO();
        orderEditDTO.setStatus(OrderStatus.OUTLETS_WAREHOUSE.getCode());
        OrderDTO orderUpdateResult = orderFeign.updateById(String.valueOf(transportOrderDto.getOrderId()), orderEditDTO);
        if (orderUpdateResult == null) {
            log.error("交件失败：订单状态更新未生效 orderId={}", transportOrderDto.getOrderId());
            return Result.error(500, "订单状态更新失败");
        }


        TaskPickupDispatchDTO pickupDispatchTaskDto = pickupDispatchTaskFeign.findByOrderId(transportOrderDto.getOrderId(), PickupDispatchTaskType.PICKUP.getCode());
        if (pickupDispatchTaskDto == null) {
            log.error("交件失败：取件任务不存在 orderId={}", transportOrderDto.getOrderId());
            return Result.error(400, "取件任务不存在");
        }
        TaskPickupDispatchDTO pickupDispatchTaskDtoUpdate = new TaskPickupDispatchDTO();
        pickupDispatchTaskDtoUpdate.setStatus(PickupDispatchTaskStatus.COMPLETED.getCode());
        pickupDispatchTaskDtoUpdate.setActualEndTime(LocalDateTime.now());
        pickupDispatchTaskDtoUpdate.setConfirmTime(LocalDateTime.now());
        TaskPickupDispatchDTO taskUpdateResult = pickupDispatchTaskFeign.updateById(pickupDispatchTaskDto.getId(), pickupDispatchTaskDtoUpdate);
        if (taskUpdateResult == null) {
            log.error("交件失败：取件任务更新未生效 taskId={}", pickupDispatchTaskDto.getId());
            return Result.error(500, "取件任务更新失败");
        }
        log.info("更新取派件任务 ID:{},PARAMS:{}", pickupDispatchTaskDto.getId(), pickupDispatchTaskDtoUpdate);

        return Result.ok();
    }

    @SneakyThrows
    @Operation(summary = "接件")
    @Parameter(name = "tranOrderId", description = "运单号", required = true, example = "")
    @ResponseBody
    @PutMapping("handover/{tranOrderId}")
    public Result handover(@PathVariable("tranOrderId") String tranOrderId) {
        log.info("接件：{}", tranOrderId);
        // id 是运单号 扫描到的内容
        TransportOrderDTO transportOrderDto = transportOrderFeign.findById(Long.valueOf(tranOrderId));
        if (ObjectUtils.isEmpty(transportOrderDto)) {
            return Result.error(400, "运单号未找到");
        }
        String orderId = String.valueOf(transportOrderDto.getOrderId());
        log.info("接件 获取运单信息：{} ,{}", tranOrderId, transportOrderDto);
        OrderDTO orderDto = orderFeign.findById(orderId);
        if (orderDto == null) {
            return Result.error(400, "订单不存在");
        }
        OrderDTO orderDTOUpdate = new OrderDTO();
        orderDTOUpdate.setStatus(OrderStatus.DISPATCHING.getCode());
        OrderDTO orderUpdateResult = orderFeign.updateById(orderDto.getId(), orderDTOUpdate);
        if (orderUpdateResult == null) {
            log.error("接件失败：订单状态更新未生效 orderId={}", orderId);
            return Result.error(500, "订单状态更新失败");
        }
        log.info("接件 修改订单状态：{} ,{}", orderDto.getId(), orderDTOUpdate);
        TaskPickupDispatchDTO pickupDispatchTaskDto = pickupDispatchTaskFeign.findByOrderId(Long.valueOf(orderId), PickupDispatchTaskType.DISPATCH.getCode());
        if (pickupDispatchTaskDto == null) {
            log.error("接件失败：派件任务不存在 orderId={}", orderId);
            return Result.error(400, "派件任务不存在");
        }
        TaskPickupDispatchDTO pickupDispatchTaskDtoUpdate = new TaskPickupDispatchDTO();
        pickupDispatchTaskDtoUpdate.setStatus(PickupDispatchTaskStatus.CONFIRM.getCode());
        pickupDispatchTaskDtoUpdate.setActualStartTime(LocalDateTime.now());
        TaskPickupDispatchDTO taskUpdateResult = pickupDispatchTaskFeign.updateById(pickupDispatchTaskDto.getId(), pickupDispatchTaskDtoUpdate);
        if (taskUpdateResult == null) {
            log.error("接件失败：派件任务更新未生效 taskId={}", pickupDispatchTaskDto.getId());
            return Result.error(500, "派件任务更新失败");
        }
        log.info("接件 修改派送任务状态：{} ,{}", pickupDispatchTaskDto.getId(), pickupDispatchTaskDtoUpdate);
        return Result.ok();
    }

    @SneakyThrows
    @Operation(summary = "妥投")
    @Parameters({
            @Parameter(name = "tranOrderId", description = "运单号", required = true, example = ""),
            @Parameter(name = "status", description = "状态 1签收 0拒收", required = true, example = "")
    })
    @ResponseBody
    @PutMapping("delivered/{tranOrderId}/{status}")
    public Result delivered(@PathVariable("tranOrderId") String tranOrderId, @PathVariable("status") String status) {
        log.info("妥投 运单号：{} ，{}", tranOrderId, status);
        boolean state = "1".equals(status); // 1签收 0拒收
        // id 是运单号 扫描到的内容
        TransportOrderDTO transportOrderDto = transportOrderFeign.findById(Long.valueOf(tranOrderId));
        if (ObjectUtils.isEmpty(transportOrderDto)) {
            return Result.error(400, "运单号未找到");
        }
        TransportOrderDTO transportOrderDtoUpdate = new TransportOrderDTO();
        transportOrderDtoUpdate.setStatus(state ? TransportOrderStatus.RECEIVED.getCode() : TransportOrderStatus.REJECTED.getCode());
        TransportOrderDTO tranOrderUpdateResult = transportOrderFeign.updateById(transportOrderDto.getId(), transportOrderDtoUpdate);
        if (tranOrderUpdateResult == null) {
            log.error("妥投失败：运单状态更新未生效 transportOrderId={}", transportOrderDto.getId());
            return Result.error(500, "运单状态更新失败");
        }
        log.info("妥投 获取运单信息：{} ,{}", transportOrderDto.getId(), transportOrderDtoUpdate);
        String orderId = String.valueOf(transportOrderDto.getOrderId());
        if (StringUtils.isBlank(orderId)) {
            return Result.error(400, "运单未关联订单");
        }
        OrderDTO orderDto = orderFeign.findById(orderId);
        if (ObjectUtils.isEmpty(orderDto)) {
            return Result.error(400, "订单不存在");
        }
        OrderDTO orderDTOUpdate = new OrderDTO();
        orderDTOUpdate.setStatus(state ? OrderStatus.RECEIVED.getCode() : OrderStatus.REJECTION.getCode());
        OrderDTO orderUpdateResult = orderFeign.updateById(orderDto.getId(), orderDTOUpdate);
        if (orderUpdateResult == null) {
            log.error("妥投失败：订单状态更新未生效 orderId={}", orderId);
            return Result.error(500, "订单状态更新失败");
        }
        log.info("妥投 修改订单状态：{} ,{}", orderDto.getId(), orderDTOUpdate);
        TaskPickupDispatchDTO pickupDispatchTaskDto = pickupDispatchTaskFeign.findByOrderId(Long.valueOf(orderId), PickupDispatchTaskType.DISPATCH.getCode());
        if (ObjectUtils.isEmpty(pickupDispatchTaskDto)) {
            return Result.error(400, "派送任务不存在");
        }
        TaskPickupDispatchDTO pickupDispatchTaskDtoUpdate = new TaskPickupDispatchDTO();
        pickupDispatchTaskDtoUpdate.setStatus(PickupDispatchTaskStatus.COMPLETED.getCode());
        pickupDispatchTaskDtoUpdate.setSignStatus(state ? PickupDispatchTaskSignStatus.RECEIVED.getCode() : PickupDispatchTaskSignStatus.REJECTION.getCode());
        pickupDispatchTaskDtoUpdate.setActualEndTime(LocalDateTime.now());
        pickupDispatchTaskDtoUpdate.setConfirmTime(LocalDateTime.now());
        TaskPickupDispatchDTO taskUpdateResult = pickupDispatchTaskFeign.updateById(pickupDispatchTaskDto.getId(), pickupDispatchTaskDtoUpdate);
        if (taskUpdateResult == null) {
            log.error("妥投失败：派件任务更新未生效 taskId={}", pickupDispatchTaskDto.getId());
            return Result.error(500, "派件任务更新失败");
        }
        log.info("妥投 修改派送任务状态：{} ,{}", pickupDispatchTaskDto.getId(), pickupDispatchTaskDtoUpdate);

        // 【P1优化】发布订单交付事件（异步处理）
        // 触发后续业务逻辑：结算流程、消息通知等
        try {
            OrderDeliveredEvent event = new OrderDeliveredEvent(
                orderId,
                toIdString(transportOrderDto.getId()),
                state,
                null,
                toIdString(pickupDispatchTaskDto.getId()),
                RequestContext.getUserId()
            );
            eventPublisher.publishOrderDelivered(event);
            log.info("[事件发布] 订单交付事件发布成功: orderId={}, signed={}", orderId, state);
        } catch (Exception e) {
            log.error("[事件发布] 订单交付事件发布失败: orderId=" + orderId, e);
            // 事件发布失败不影响主流程
        }

        return Result.ok();
    }

    /**
     * @deprecated 旧版 GET 接口会在 GET 请求上执行写操作，可被直接构造链接触发，仅保留兼容；新前端请走 POST
     */
    @Deprecated
    @SneakyThrows
    @Operation(summary = "验证身份证号是否合法(旧GET接口，已废弃，请改用POST)")
    @Parameters({
            @Parameter(name = "orderNumber", description = "订单号", required = true, example = ""),
            @Parameter(name = "code", description = "身份证号", required = true, example = "")
    })
    @ResponseBody
    @GetMapping("verifyIdCard")
    public Result verifyIdCardByGet(@RequestParam String orderNumber, @RequestParam String code) {
        return doVerifyIdCard(orderNumber, code);
    }

    @SneakyThrows
    @Operation(summary = "验证身份证号是否合法")
    @Parameters({
            @Parameter(name = "orderNumber", description = "订单号", required = true, example = ""),
            @Parameter(name = "code", description = "身份证号", required = true, example = "")
    })
    @ResponseBody
    @PostMapping("verifyIdCard")
    public Result verifyIdCard(@RequestParam String orderNumber, @RequestParam String code) {
        return doVerifyIdCard(orderNumber, code);
    }

    /**
     * 身份证号写入与合法性校验的实际实现。
     * 归属校验：订单必须存在当前快递员负责的非取消取派任务。
     * 身份证号仅允许首次写入，已有值则拒绝覆盖。
     */
    private Result doVerifyIdCard(String orderNumber, String code) {
        log.info("身份证号验证OrderId：{} Code:{} ", orderNumber, code);
        if (code == null || code.length() > 18 || code.length() < 15) {
            return Result.error(400, "身份证号不符合要求");
        }
        String regex = "\\d{15}(\\d{2}[0-9xX])?";

        if (!code.matches(regex)) {
            return Result.error(400, "身份证号不符合要求");
        }

        // 归属校验：按订单号查全部取派任务，当前快递员必须负责其中一个非取消任务
        TaskPickupDispatchDTO taskQuery = new TaskPickupDispatchDTO();
        taskQuery.setOrderId(Long.valueOf(orderNumber));
        List<TaskPickupDispatchDTO> orderTasks = pickupDispatchTaskFeign.findAll(taskQuery);
        String currentUserId = RequestContext.getUserId();
        String taskOwner = null;
        if (orderTasks != null) {
            for (TaskPickupDispatchDTO task : orderTasks) {
                if (PickupDispatchTaskStatus.CANCELLED.getCode().equals(task.getStatus())) {
                    continue;
                }
                if (currentUserId != null && currentUserId.equals(toIdString(task.getCourierId()))) {
                    // 命中当前用户负责的任务，直接确定归属
                    taskOwner = currentUserId;
                    break;
                }
                if (taskOwner == null) {
                    taskOwner = toIdString(task.getCourierId());
                }
            }
        }
        Result deny = OwnershipAssert.checkEquals(taskOwner, currentUserId, "无权操作该订单");
        if (deny != null) {
            return deny;
        }

        OrderDTO order = orderFeign.findById(orderNumber);
        if (order == null || StringUtils.isBlank(order.getMemberId())) {
            return Result.error(400, "订单不存在");
        }
        String memberId = order.getMemberId();
        // 身份证号仅允许首次写入：已有非空值则拒绝覆盖，防止冒用他人工号篡改
        MemberDTO existingMember = memberFeign.detail(memberId);
        if (existingMember != null && StringUtils.isNotBlank(existingMember.getIdCardNo())) {
            log.warn("身份证号已存在，拒绝重复写入 memberId={}", memberId);
            return Result.error(400, "身份证号已存在，不允许重复写入");
        }
        log.info("身份证号验证MemberId：{} ", memberId);
        MemberDTO member = new MemberDTO();
        member.setId(memberId);
        member.setIdCardNo(code);
        member.setIdCardNoVerify(MemberIdCardVerifyStatus.NONE.getCode());
        Result updateResult = memberFeign.update(memberId, member);
        if (updateResult == null || !"0".equals(String.valueOf(updateResult.get("code")))) {
            log.error("身份证号写入失败 memberId={}, result={}", memberId, updateResult);
            return Result.error(500, "身份证号写入失败");
        }
        log.info("更新身份证号：{}", member);
        String checkResult = IdCardUtils.IdentityCardVerification(code);
        if (StringUtils.isNotBlank(checkResult)) {
            member.setIdCardNoVerify(MemberIdCardVerifyStatus.FAIL.getCode());
            Result failUpdateResult = memberFeign.update(memberId, member);
            if (failUpdateResult == null || !"0".equals(String.valueOf(failUpdateResult.get("code")))) {
                log.error("身份证校验状态(FAIL)更新失败 memberId={}, result={}", memberId, failUpdateResult);
            }
            log.info("更新身份证号 FAIL：{}", member);
            return Result.error(400, checkResult);
        }
        // 验证通过 写入客户端
        member.setIdCardNoVerify(MemberIdCardVerifyStatus.SUCCESS.getCode());
        Result successUpdateResult = memberFeign.update(memberId, member);
        if (successUpdateResult == null || !"0".equals(String.valueOf(successUpdateResult.get("code")))) {
            log.error("身份证校验状态(SUCCESS)更新失败 memberId={}, result={}", memberId, successUpdateResult);
            return Result.error(500, "身份证校验状态更新失败");
        }
        log.info("更新身份证号 SUCCESS：{}", member);
        return Result.ok();
    }


    @SneakyThrows
    @Operation(summary = "路由")
    @Parameter(name = "id", description = "主键", required = true, example = "")
    @ResponseBody
    @GetMapping("route")
    public Result route(String id) {
        log.info("路由信息 ID：{}", id);
        try {
            TaskPickupDispatchDTO pickupDispatchTaskDTO = pickupDispatchTaskFeign.findById(Long.valueOf(id));
            log.info("路由信息 TaskPickupDispatchDTO：{}", pickupDispatchTaskDTO);
            if (pickupDispatchTaskDTO == null) {
                return Result.error(404, "取派件任务不存在");
            }
            // 归属校验：取派任务必须属于当前登录快递员，防止越权拉取他人订单路由
            Result deny = OwnershipAssert.checkEquals(toIdString(pickupDispatchTaskDTO.getCourierId()), RequestContext.getUserId(), "无权查看他人任务路由");
            if (deny != null) {
                return deny;
            }
            String orderId = String.valueOf(pickupDispatchTaskDTO.getOrderId());

            TransportOrderDTO transportOrderDTO = transportOrderFeign.findByOrderId(Long.valueOf(orderId));
            log.info("路由信息 TransportOrderDTO：{}", transportOrderDTO);
            if (transportOrderDTO == null) {
                return Result.error(404, "运单不存在");
            }

            List<TaskTransportDTO> transportTaskDTOs = transportTaskFeign.findAllByOrderIdOrTaskId(transportOrderDTO.getId(), null);
            if (transportTaskDTOs == null) {
                transportTaskDTOs = new ArrayList<>();
            }

            Set<Long> agencySet = new HashSet<>();
            agencySet.addAll(transportTaskDTOs.stream().map(TaskTransportDTO::getStartOrgId).filter(Objects::nonNull).collect(Collectors.toSet()));
            agencySet.addAll(transportTaskDTOs.stream().map(TaskTransportDTO::getEndOrgId).filter(Objects::nonNull).collect(Collectors.toSet()));

            CompletableFuture<Map<Long, OrgDTO>> orgMapFeture = PdCompletableFuture.agencyMapFuture(orgFeign, null, agencySet, null);
            Map<Long, OrgDTO> orgMap = orgMapFeture.get();
            log.info("路由信息 AgencyMapFuture：{}", orgMap);

            List<RouteDTO> routeArray = new ArrayList<>();

            transportTaskDTOs.stream().forEach(item -> {
                if (null != item.getActualPickUpTime()) {
                    // 修改点：orgMap 可能不含该机构 id，get 返回 null，先判空避免 NPE
                    OrgDTO startOrg = orgMap.get(item.getStartOrgId());
                    routeArray.add(RouteDTO.builder()
                            .arrivalTime(item.getActualPickUpTime())
                            .agencyName("快递在【" + (startOrg != null ? startOrg.getName() : "") + "】已装车，准备发往下一站")
                            .build());
                }
                if (null != item.getActualArrivalTime()) {
                    // 修改点：orgMap 可能不含该机构 id，get 返回 null，先判空避免 NPE
                    OrgDTO endOrg = orgMap.get(item.getEndOrgId());
                    routeArray.add(RouteDTO.builder()
                            .arrivalTime(item.getActualArrivalTime())
                            .agencyName("快递已到达【" + (endOrg != null ? endOrg.getName() : "") + "】")
                            .build());
                }
            });

            Collections.reverse(routeArray);
            routeArray.forEach(item -> {
                log.info("路由信息：{}", item);
            });
            return Result.ok().put("data", routeArray);
        } catch (Exception e) {
            // 不再吞异常返回空数组（前端会误判为"无路由"），明确返回 4xx 让前端感知失败
            log.error("路由查询失败 id={}", id, e);
            return Result.error(400, "路由信息查询失败");
        }
    }


    private OrderDTO buildOrderAndPrice(MailingSaveDTO entity) {

        OrderDTO orderAddDto = orderFeign.findById(entity.getOrderNumber());

        orderAddDto.setPaymentMethod(entity.getPayMethod());
        orderAddDto.setPaymentStatus(1); // 默认未付款

        orderAddDto.setOrderType(orderAddDto.getReceiverCityId().equals(orderAddDto.getSenderCityId()) ? OrderType.INCITY.getCode() : OrderType.OUTCITY.getCode());
        // 总价由订单服务(orderFeign.getOrderMsg)按距离+重量实时计算
        OrderCargoDto cargoDto = buildOrderCargo(entity);
        orderAddDto.setOrderCargoDto(cargoDto);
        Map map = orderFeign.getOrderMsg(orderAddDto);
        orderAddDto.setAmount(new BigDecimal(map.getOrDefault("amount", "20").toString()));
        return orderAddDto;
    }

    private OrderCargoDto buildOrderCargo(MailingSaveDTO entity) {
        OrderCargoDto cargoDto = new OrderCargoDto();
        cargoDto.setName(entity.getGoodsName());
        cargoDto.setGoodsTypeId(entity.getGoodsType());
        cargoDto.setWeight(new BigDecimal(entity.getGoodsWeight()));
        cargoDto.setQuantity(1);
        cargoDto.setTotalWeight(cargoDto.getWeight().multiply(new BigDecimal(cargoDto.getQuantity())));
        return cargoDto;
    }

    /**
     * 预估总价
     *
     * @param entity
     * @return
     */
    @Operation(summary = "预估总价")
    @PostMapping("totalPrice")
    @ResponseBody
    public Result totalPrice(@RequestBody MailingSaveDTO entity) {
        log.info("计算预估总价：{}", entity);
        OrderDTO orderAddDto = buildOrderAndPrice(entity);
        return Result.ok().put("amount", orderAddDto.getAmount());
    }

    /**
     * 配送环节异常上报（D-41 · 定案 T-3 · 2026-10-08 新增）
     *
     * <p>快递员在App 上主动上报破损/拒收/地址错误，<b>落库复用 pd-netty 的
     * {@code pd_alarm_record}</b>——与 GPS 自动告警同表，按 alarmType 区分来源。</p>
     *
     * <p><b>入参遵循 D-46「类型 + 照片 + 备注」三要素</b>，由 pd-netty 侧统一校验；
     * 无照片凭证的异常不予受理。</p>
     *
     * <p><b>上报人ID 由服务端从 token 取</b>（{@link RequestContext#getUserId()}），
     * 不接受前端传他人 ID——否则可冒名上报。</p>
     *
     * @param dto 异常上报入参
     * @return 落库后的告警记录（含 id 供前端追单）
     */
    @Operation(summary = "异常上报（破损/拒收/地址错误）")
    @PostMapping("exception/report")
    public Result reportException(@RequestBody ExceptionReportFeignDTO dto) {
        String courierId = RequestContext.getUserId();
        if (dto == null) {
            return Result.error(400, "上报内容不能为空");
        }
        // 服务端覆盖上报人：绝不相信前端传入的 reporterId（防冒名上报）
        dto.setReporterId(courierId);
        dto.setCourier(true);
        Result result = exceptionReportFeign.report(dto);
        log.info("快递员异常上报: courierId={}, type={}", courierId, dto.getExceptionType());
        return result;
    }

    /**
     * work 实体 ID 为 Long，归属校验/事件等保持 String 签名，在调用边界安全转换（null 透传）。
     */
    private static String toIdString(Long id) {
        return id == null ? null : String.valueOf(id);
    }
}
