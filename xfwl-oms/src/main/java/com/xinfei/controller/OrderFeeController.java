package com.xinfei.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinfei.common.context.RequestContext;
import com.xinfei.common.utils.IdConverter;
import com.xinfei.common.utils.Result;
import com.xinfei.entity.OrderFee;
import com.xinfei.service.IOrderFeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单附加费明细内部接口 —— POD-03 第一期
 *
 * <p>数据/服务层，网关无 /api/oms 路由；对外由 xfwl-web-manager 聚合
 * {@code /api/manager/order-fee/*} 暴露（见 OrderFeeFeign）。
 * 明细独立存表不改写 order.amount。</p>
 */
@Slf4j
@RestController
@RequestMapping("order-fee")
@Tag(name = "订单附加费明细")
public class OrderFeeController {

    @Autowired
    private IOrderFeeService orderFeeService;

    private Long currentOperator() {
        String userId = RequestContext.getUserId();
        return StringUtils.isBlank(userId) ? null : IdConverter.toLong(userId);
    }

    private String currentOperatorName() {
        String name = RequestContext.getUserName();
        return StringUtils.isBlank(name) ? null : name;
    }

    private Result denyIfNoIdentity() {
        if (StringUtils.isBlank(RequestContext.getUserId())) {
            return Result.error(401, "未登录或身份信息缺失");
        }
        return null;
    }

    @Operation(summary = "按订单查明细+合计")
    @GetMapping("/list")
    public Result list(@RequestParam("orderId") String orderId) {
        Result deny = denyIfNoIdentity();
        if (deny != null) {
            return deny;
        }
        List<OrderFee> items = orderFeeService.listByOrderId(orderId);
        BigDecimal total = orderFeeService.sumByOrderId(orderId);
        Map<String, Object> data = new HashMap<>();
        data.put("items", items);
        data.put("total", total);
        return Result.ok().put("data", data);
    }

    @Operation(summary = "分页查询（orderId 必填）")
    @GetMapping("/page")
    public Result page(@RequestParam("orderId") String orderId,
                       @RequestParam(value = "page", required = false) Integer page,
                       @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        Result deny = denyIfNoIdentity();
        if (deny != null) {
            return deny;
        }
        Page<OrderFee> p = new Page<>(page == null || page < 1 ? 1 : page,
                pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 500));
        orderFeeService.page(p, new LambdaQueryWrapper<OrderFee>()
                .eq(OrderFee::getOrderId, orderId)
                .orderByAsc(OrderFee::getCreateTime));
        return Result.ok().put("data", p);
    }

    @Operation(summary = "人工补录附加费")
    @PostMapping
    public Result addManual(@RequestBody OrderFee fee) {
        Result deny = denyIfNoIdentity();
        if (deny != null) {
            return deny;
        }
        try {
            OrderFee saved = orderFeeService.addManual(fee, currentOperator(), currentOperatorName());
            return Result.ok().put("data", saved);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @Operation(summary = "按配置自动生成附加费明细（幂等）")
    @PostMapping("/calculate/{orderId}")
    public Result calculate(@PathVariable("orderId") Long orderId) {
        Result deny = denyIfNoIdentity();
        if (deny != null) {
            return deny;
        }
        try {
            List<OrderFee> created = orderFeeService.calculateForOrder(orderId, currentOperator(), currentOperatorName());
            return Result.ok().put("data", created);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }
}
