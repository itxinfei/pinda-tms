package com.xinfei.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xinfei.common.utils.Result;
import com.xinfei.feign.OrderFeeFeign;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理端订单附加费明细（POD-03 第一期）—— 薄转发至 xf-oms，
 * 网关前缀 /api/manager/order-fee。权限码具体码禁通配。
 */
@RestController
@RequestMapping("order-fee")
@RequiredArgsConstructor
@Tag(name = "订单附加费明细")
public class OrderFeeController {

    private final OrderFeeFeign orderFeeFeign;

    @Operation(summary = "按订单查明细+合计")
    @GetMapping("/list")
    @SaCheckPermission("finance:order-fee:list")
    public Result list(@RequestParam("orderId") String orderId) {
        return orderFeeFeign.list(orderId);
    }

    @Operation(summary = "分页查询（orderId 必填）")
    @GetMapping("/page")
    @SaCheckPermission("finance:order-fee:list")
    public Result page(@RequestParam("orderId") String orderId,
                       @RequestParam(value = "page", required = false) Integer page,
                       @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return orderFeeFeign.page(orderId, page, pageSize);
    }

    @Operation(summary = "人工补录附加费")
    @PostMapping
    @SaCheckPermission("finance:order-fee:add")
    public Result addManual(@RequestBody Map<String, Object> body) {
        return orderFeeFeign.addManual(body);
    }

    @Operation(summary = "按配置自动生成附加费明细（幂等）")
    @PostMapping("/calculate/{orderId}")
    @SaCheckPermission("finance:order-fee:add")
    public Result calculate(@PathVariable("orderId") Long orderId) {
        return orderFeeFeign.calculate(orderId);
    }
}
