package com.xinfei.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xinfei.common.utils.Result;
import com.xinfei.feign.OrderExceptionFeign;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端订单异常（ORD-04）—— 薄转发至 xfwl-oms，网关前缀 /api/manager/order-exception。
 *
 * <p>网关无 /api/oms 路由，内部服务 xfwl-oms 须由此聚合暴露。
 * 权限码 {@code order:exception}（sys_menu type=B 按钮行，ADMIN 放行 *）。</p>
 */
@RestController
@RequestMapping("order-exception")
@RequiredArgsConstructor
@Tag(name = "订单异常(ORD-04)")
public class OrderExceptionController {

    private final OrderExceptionFeign orderExceptionFeign;

    @Operation(summary = "登记订单异常（不改订单状态）")
    @PostMapping("/register")
    @SaCheckPermission("order:exception")
    public Result register(@RequestParam("orderId") Long orderId,
                           @RequestParam("exceptionType") Integer exceptionType,
                           @RequestParam("description") String description) {
        return orderExceptionFeign.register(orderId, exceptionType, description);
    }

    @Operation(summary = "处理异常（待处理→已处理，留痕）")
    @PostMapping("/{id}/handle")
    @SaCheckPermission("order:exception")
    public Result handle(@PathVariable("id") Long id,
                         @RequestParam("handleResult") String handleResult) {
        return orderExceptionFeign.handle(id, handleResult);
    }

    @Operation(summary = "分页查询")
    @GetMapping("/page")
    @SaCheckPermission("order:exception")
    public Result page(@RequestParam(value = "page", required = false) Integer page,
                       @RequestParam(value = "pageSize", required = false) Integer pageSize,
                       @RequestParam(value = "orderId", required = false) Long orderId,
                       @RequestParam(value = "exceptionType", required = false) Integer exceptionType,
                       @RequestParam(value = "handleStatus", required = false) Integer handleStatus) {
        return orderExceptionFeign.page(page, pageSize, orderId, exceptionType, handleStatus);
    }
}
