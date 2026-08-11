package com.itheima.pinda.controller;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.google.common.collect.Maps;
import com.itheima.pinda.DTO.OrderDTO;
import com.itheima.pinda.DTO.OrderLocationDto;
import com.itheima.pinda.DTO.OrderSearchDTO;
import com.itheima.pinda.common.exception.PdException;
import com.itheima.pinda.common.utils.Result;
import com.itheima.pinda.DTO.OrderCargoDto;
import com.itheima.pinda.entity.OrderCargo;
import com.itheima.pinda.service.IOrderCargoService;
import com.itheima.pinda.enums.OrderPaymentStatus;
import com.itheima.pinda.common.utils.CustomIdGenerator;
import com.itheima.pinda.common.utils.PageResponse;
import com.itheima.pinda.entity.Order;
import com.itheima.pinda.entity.OrderLocation;
import com.itheima.pinda.service.IOrderLocationService;
import com.itheima.pinda.service.IOrderService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 订单
 */
@Log4j2
@RestController
@RequestMapping("order")
public class OrderController {
    @Autowired
    private IOrderService orderService;
    @Autowired
    private IOrderLocationService orderLocationService;
    @Autowired
    private IOrderCargoService orderCargoService;
    @Autowired
    private CustomIdGenerator idGenerator;

    /**
     * 新增订单
     *
     * @param orderDTO 订单信息
     * @return 订单信息
     */
    @PostMapping("")
    public OrderDTO save(@RequestBody OrderDTO orderDTO, HttpServletResponse res) {
        log.info("保存订单信息:{}", JSON.toJSONString(orderDTO));
        if (orderDTO == null || orderDTO.getOrderCargoDto() == null) {
            log.warn("[订单] 保存失败：orderDTO或orderCargoDto为空");
            return null;
        }
        Order order = new Order();
        order.setEstimatedArrivalTime(LocalDateTime.now().plus(2, ChronoUnit.DAYS));
        Map map = orderService.calculateAmount(orderDTO);
        if (map == null || map.get("orderDto") == null) {
            log.error("[订单] 计算运费失败，无法保存订单");
            return null;
        }
        log.info("实时计算运费:{}", map);
        orderDTO = (OrderDTO) map.get("orderDto");
        BeanUtils.copyProperties(orderDTO, order);
        if ("sender error msg".equals(orderDTO.getSenderAddress()) || "receiver error msg".equals(orderDTO.getReceiverAddress())) {
            log.warn("[订单] 地址校验失败，订单未入库: senderAddr={}, receiverAddr={}",
                orderDTO.getSenderAddress(), orderDTO.getReceiverAddress());
            return orderDTO;
        }
        if (!map.containsKey("amount")) {
            log.error("[订单] 价格计算异常，未返回金额: orderId={}", orderDTO.getId());
            throw new PdException("运费计算结果异常，请稍后重试");
        }
        order.setAmount(new BigDecimal(map.get("amount").toString()));
        orderService.saveOrder(order);
        log.info("订单信息入库:{}", order);

        // 【P0修复】订单入库后同步保存货物明细，避免订单缺少货物数据
        OrderCargoDto cargoDto = orderDTO.getOrderCargoDto();
        if (cargoDto != null) {
            OrderCargo orderCargo = new OrderCargo();
            BeanUtils.copyProperties(cargoDto, orderCargo);
            orderCargo.setId(null);
            orderCargo.setOrderId(order.getId());
            orderCargo.setTranOrderId(null);
            // 缺失总重量时按 重量×数量 兜底计算
            if (orderCargo.getTotalWeight() == null && orderCargo.getWeight() != null && orderCargo.getQuantity() != null) {
                orderCargo.setTotalWeight(orderCargo.getWeight().multiply(new BigDecimal(orderCargo.getQuantity())));
            }
            orderCargoService.saveSelective(orderCargo);
            log.info("订单货物明细入库:{}", orderCargo);
        }

        OrderDTO result = new OrderDTO();
        BeanUtils.copyProperties(order, result);
        return result;
    }


    @PostMapping("orderMsg")
    public Map save(@RequestBody OrderDTO orderDTO) {
        Map map = Maps.newHashMap();
        if (orderDTO == null) {
            return map;
        }
        if (orderDTO.getOrderCargoDto() == null) {
            return map;
        }
        BigDecimal bigDecimal = orderDTO.getOrderCargoDto().getTotalWeight() == null ? BigDecimal.ZERO : orderDTO.getOrderCargoDto().getTotalWeight();
        if (bigDecimal.compareTo(BigDecimal.ZERO) < 1) {
            return map;
        }
        //根据重量和距离
        map = orderService.calculateAmount(orderDTO);
        return map;
    }

    /**
     * 修改订单信息
     *
     * @param id       订单id
     * @param orderDTO 订单信息
     * @return 订单信息
     */
    @PutMapping("/{id}")
    public OrderDTO updateById(@PathVariable(name = "id") String id, @RequestBody OrderDTO orderDTO) {
        orderDTO.setId(id);
        // 【安全加固】通用更新端点屏蔽敏感字段，避免任意调用方篡改：
        // - amount 金额由服务端 calculateAmount 计算（改价请走专用 /{id}/reprice 端点）；
        // - paymentStatus 支付状态由支付流程控制（支付请走专用 /{id}/pay 端点）；
        // - estimatedArrivalTime/createTime 由系统计算与创建流程维护；
        // - 状态字段由 OrderServiceImpl 状态机校验保护（合法流转时才允许变更）。
        orderDTO.setAmount(null);
        orderDTO.setPaymentStatus(null);
        orderDTO.setEstimatedArrivalTime(null);
        orderDTO.setCreateTime(null);
        Order order = new Order();
        BeanUtils.copyProperties(orderDTO, order);
        // 状态流转校验失败时返回 null，由 Feign 调用方感知（与 TransportOrder 行为一致）
        if (!orderService.updateById(order)) {
            log.warn("[订单] 更新失败（可能状态流转不合法或订单不存在）: id={}, status={}", id, orderDTO.getStatus());
            return null;
        }
        return orderDTO;
    }

    /**
     * 订单支付确认（专用端点，供客户支付流程调用）
     *
     * <p>服务端校验订单存在且未支付后置支付状态为已支付，不接受请求体传参，避免伪造支付状态。
     * 注意：调用方（pd-web-customer MailingController.pay）需自行校验当前用户为该订单归属人。</p>
     *
     * @param id 订单id
     * @return 支付结果
     */
    @PutMapping("/{id}/pay")
    public Result pay(@PathVariable(name = "id") String id) {
        if (StringUtils.isBlank(id)) {
            return Result.error(400, "订单ID不能为空");
        }
        Order order = orderService.getById(id);
        if (order == null) {
            return Result.error(400, "订单不存在");
        }
        // 幂等保护：已支付订单不允许重复支付
        if (OrderPaymentStatus.PAID.getStatus().equals(order.getPaymentStatus())) {
            log.warn("[订单] 订单已支付，拒绝重复支付: id={}", id);
            return Result.error(400, "订单已支付，请勿重复操作");
        }
        OrderDTO update = new OrderDTO();
        update.setId(id);
        update.setPaymentStatus(OrderPaymentStatus.PAID.getStatus());
        Order orderUpdate = new Order();
        BeanUtils.copyProperties(update, orderUpdate);
        if (!orderService.updateById(orderUpdate)) {
            log.error("[订单] 支付状态更新失败: id={}", id);
            return Result.error(500, "支付状态更新失败");
        }
        log.info("[订单] 支付确认成功: id={}", id);
        return Result.ok();
    }

    /**
     * 订单改价（专用端点，供客户编辑订单流程调用）
     *
     * <p>金额由服务端 calculateAmount 重算，不接受客户端传入的 amount，避免价格篡改。</p>
     *
     * @param id       订单id
     * @param orderDTO 订单信息（用于重算运费）
     * @return 更新后的订单金额
     */
    @PutMapping("/{id}/reprice")
    public Result reprice(@PathVariable(name = "id") String id, @RequestBody OrderDTO orderDTO) {
        if (orderDTO == null) {
            return Result.error(400, "订单信息不能为空");
        }
        orderDTO.setId(id);
        // 服务端重算运费
        Map map = orderService.calculateAmount(orderDTO);
        // 【健壮性】calculateAmount 在地址解析失败时返回含 errorMsg 的 map（amount=0），
        // 必须一并拒绝，避免把 0 元金额持久化（与 save 端点的地址错误处理保持一致）
        if (map == null || !map.containsKey("amount") || map.containsKey("errorMsg")) {
            log.error("[订单] 运费重算失败: id={}, map={}", id, map);
            return Result.error(500, "运费重算失败");
        }
        BigDecimal amount = new BigDecimal(map.get("amount").toString());
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.error("[订单] 运费重算金额非法: id={}, amount={}", id, amount);
            return Result.error(500, "运费重算结果异常");
        }
        Order orderUpdate = new Order();
        orderUpdate.setId(id);
        orderUpdate.setAmount(amount);
        if (!orderService.updateById(orderUpdate)) {
            log.error("[订单] 改价更新失败: id={}", id);
            return Result.error(500, "改价更新失败");
        }
        log.info("[订单] 改价成功: id={}, amount={}", id, amount);
        return Result.ok().put("amount", amount);
    }

    /**
     * 获取订单分页数据
     *
     * @param orderDTO 查询条件
     * @return 订单分页数据
     */
    @PostMapping("/page")
    public PageResponse<OrderDTO> findByPage(@RequestBody OrderDTO orderDTO) {
        Order queryOrder = new Order();
        BeanUtils.copyProperties(orderDTO, queryOrder);
        IPage<Order> orderIPage = orderService.findByPage(orderDTO.getPage(), orderDTO.getPageSize(), queryOrder);
        List<OrderDTO> dtoList = new ArrayList<>();
        orderIPage.getRecords().forEach(order -> {
            OrderDTO dto = new OrderDTO();
            BeanUtils.copyProperties(order, dto);
            dtoList.add(dto);
        });
        return PageResponse.<OrderDTO>builder().items(dtoList).pagesize(orderDTO.getPageSize()).page(orderDTO.getPage()).counts(orderIPage.getTotal())
                .pages(orderIPage.getPages()).build();
    }

    /**
     * 根据id获取订单详情
     *
     * @param id 订单Id
     * @return 订单详情
     */
    @GetMapping("/{id}")
    public OrderDTO findById(@PathVariable(name = "id") String id) {
        OrderDTO orderDTO = new OrderDTO();
        Order order = orderService.getById(id);

        if (order != null) {
            BeanUtils.copyProperties(order, orderDTO);
        } else {
            orderDTO = null;
        }
        return orderDTO;
    }

    /**
     * 根据id获取集合
     *
     * @param ids 订单Id
     * @return 订单详情
     */
    @GetMapping("ids")
    public List<OrderDTO> findById(@RequestParam(name = "ids") List<String> ids) {
        List<Order> orders = orderService.listByIds(ids);
        return orders.stream().map(item -> {
            OrderDTO orderDTO = new OrderDTO();
            BeanUtils.copyProperties(item, orderDTO);
            return orderDTO;
        }).collect(Collectors.toList());
    }

    @ResponseBody
    @RequestMapping(value = "pageLikeForCustomer", method = RequestMethod.POST)
    public PageResponse<OrderDTO> pageLikeForCustomer(@RequestBody OrderSearchDTO orderSearchDTO) {

        //查询结果
        IPage<Order> orderIPage = orderService.pageLikeForCustomer(orderSearchDTO);
        List<OrderDTO> dtoList = new ArrayList<>();
        orderIPage.getRecords().forEach(order -> {
            OrderDTO dto = new OrderDTO();
            BeanUtils.copyProperties(order, dto);
            dtoList.add(dto);
        });

        return PageResponse.<OrderDTO>builder().items(dtoList).pagesize(orderSearchDTO.getPageSize()).page(orderSearchDTO.getPage()).counts(orderIPage.getTotal())
                .pages(orderIPage.getPages()).build();
    }

    @ResponseBody
    @PostMapping("list")
    public List<Order> list(@RequestBody OrderSearchDTO orderSearchDTO) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(orderSearchDTO.getStatus() != null, Order::getStatus, orderSearchDTO.getStatus());
        wrapper.in(!CollectionUtils.isEmpty(orderSearchDTO.getReceiverCountyIds()), Order::getReceiverCountyId, orderSearchDTO.getReceiverCountyIds());
        wrapper.in(!CollectionUtils.isEmpty(orderSearchDTO.getSenderCountyIds()), Order::getSenderCountyId, orderSearchDTO.getSenderCountyIds());
        wrapper.eq(StringUtils.isNotEmpty(orderSearchDTO.getCurrentAgencyId()), Order::getCurrentAgencyId, orderSearchDTO.getCurrentAgencyId());

        return orderService.list(wrapper);
    }


    @ResponseBody
    @PostMapping("location/saveOrUpdate")
    public OrderLocationDto saveOrUpdateLoccation(@RequestBody OrderLocationDto orderLocationDto) {
        String orderId = orderLocationDto.getOrderId();
        if (StringUtils.isBlank(orderId)) {
            log.warn("[订单] 位置保存失败：orderId为空");
            return orderLocationDto;
        }
        OrderLocation exist = orderLocationService.getBaseMapper()
            .selectOne(new LambdaQueryWrapper<OrderLocation>().eq(OrderLocation::getOrderId, orderId));
        if (exist != null) {
            OrderLocation orderLocationUpdate = new OrderLocation();
            BeanUtils.copyProperties(orderLocationDto, orderLocationUpdate);
            orderLocationUpdate.setId(exist.getId());
            orderLocationService.getBaseMapper().updateById(orderLocationUpdate);
        } else {
            OrderLocation orderLocation = new OrderLocation();
            BeanUtils.copyProperties(orderLocationDto, orderLocation);
            orderLocation.setId(idGenerator.nextId(orderLocation).toString());
            orderLocationService.save(orderLocation);
        }
        return orderLocationDto;
    }

    @GetMapping("orderId")
    public OrderLocationDto selectByOrderId(@RequestParam(name = "orderId") String orderId) {
        OrderLocationDto result = new OrderLocationDto();
        OrderLocation location = orderLocationService.getBaseMapper().selectOne(new QueryWrapper<OrderLocation>().eq("order_id", orderId).last(" limit 1"));
        if (location != null) {
            BeanUtils.copyProperties(location, result);
        }
        return result;
    }

    @PostMapping("del")
    public int deleteOrderLocation(@RequestBody OrderLocationDto orderLocationDto) {
        String orderId = orderLocationDto.getOrderId();
        int result = 0;
        if (StringUtils.isNotBlank(orderId)) {
            result = orderLocationService.getBaseMapper().delete(new QueryWrapper<OrderLocation>().eq("order_id", orderId));
        }
        return result;
    }
}

