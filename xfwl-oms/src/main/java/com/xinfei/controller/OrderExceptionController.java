package com.xinfei.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xinfei.common.context.RequestContext;
import com.xinfei.common.utils.IdConverter;
import com.xinfei.common.utils.Result;
import com.xinfei.entity.OrderException;
import com.xinfei.service.IOrderExceptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 订单异常记录内部接口 —— ORD-04
 *
 * <p>数据/服务层，网关无 /api/oms 路由；对外由 xfwl-web-manager 聚合
 * {@code /api/manager/order-exception/*} 暴露（见 OrderExceptionFeign）。
 * 不新增订单状态值（守《07》紧凑 1~12 口径），异常以独立表标记+留痕。</p>
 */
@Slf4j
@RestController
@RequestMapping("order-exception")
@Tag(name = "订单异常(ORD-04)")
public class OrderExceptionController {

    @Autowired
    private IOrderExceptionService orderExceptionService;

    private Long currentOperator() {
        String userId = RequestContext.getUserId();
        return StringUtils.isBlank(userId) ? null : IdConverter.toLong(userId);
    }

    private Result denyIfNoIdentity() {
        if (StringUtils.isBlank(RequestContext.getUserId())) {
            return Result.error(401, "未登录或身份信息缺失");
        }
        return null;
    }

    @Operation(summary = "登记订单异常（不改订单状态）")
    @PostMapping("/register")
    public Result register(@RequestParam("orderId") Long orderId,
                           @RequestParam("exceptionType") Integer exceptionType,
                           @RequestParam("description") String description) {
        Result deny = denyIfNoIdentity();
        if (deny != null) {
            return deny;
        }
        try {
            OrderException entity = orderExceptionService.register(orderId, exceptionType, description, currentOperator());
            return Result.ok().put("data", entity);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @Operation(summary = "处理异常（待处理→已处理，留痕）")
    @PostMapping("/{id}/handle")
    public Result handle(@PathVariable("id") Long id,
                         @RequestParam("handleResult") String handleResult) {
        Result deny = denyIfNoIdentity();
        if (deny != null) {
            return deny;
        }
        try {
            boolean ok = orderExceptionService.handle(id, handleResult, RequestContext.getUserName());
            return ok ? Result.ok() : Result.error(400, "处理失败：记录不存在或已处理");
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @Operation(summary = "分页查询")
    @GetMapping("/page")
    public Result page(@RequestParam(value = "page", required = false) Integer page,
                       @RequestParam(value = "pageSize", required = false) Integer pageSize,
                       @RequestParam(value = "orderId", required = false) Long orderId,
                       @RequestParam(value = "exceptionType", required = false) Integer exceptionType,
                       @RequestParam(value = "handleStatus", required = false) Integer handleStatus) {
        Result deny = denyIfNoIdentity();
        if (deny != null) {
            return deny;
        }
        IPage<OrderException> p = orderExceptionService.findByPage(page, pageSize, orderId, exceptionType, handleStatus);
        return Result.ok().put("data", p);
    }
}
