package com.itheima.pinda.controller;

import com.itheima.pinda.DTO.OrderDTO;
import com.itheima.pinda.DTO.TaskPickupDispatchDTO;
import com.itheima.pinda.DTO.TransportOrderDTO;
import com.itheima.pinda.common.utils.PageResponse;
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

import java.util.List;
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
}
