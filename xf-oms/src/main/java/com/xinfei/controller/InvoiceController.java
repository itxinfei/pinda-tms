package com.xinfei.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinfei.common.context.RequestContext;
import com.xinfei.common.utils.IdConverter;
import com.xinfei.common.utils.Result;
import com.xinfei.entity.Invoice;
import com.xinfei.service.IInvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 电子发票（开票）内部接口 —— P0-6c
 *
 * <p>数据/服务层，网关无 /api/oms 路由；对外由 xf-web-manager 聚合
 * {@code /api/manager/invoice/*} 暴露（见 InvoiceFeign）。不接真实税控。</p>
 */
@Slf4j
@RestController
@RequestMapping("invoice")
@Tag(name = "电子发票(开票)")
public class InvoiceController {

    @Autowired
    private IInvoiceService invoiceService;

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

    @Operation(summary = "申请开票（按账单）")
    @PostMapping("/apply")
    public Result apply(@RequestParam("billId") Long billId,
                        @RequestParam(value = "type", required = false) Integer type,
                        @RequestParam(value = "taxNo", required = false) String taxNo,
                        @RequestParam(value = "mailAddress", required = false) String mailAddress) {
        Result deny = denyIfNoIdentity();
        if (deny != null) {
            return deny;
        }
        try {
            Invoice inv = invoiceService.apply(billId, type, taxNo, mailAddress, currentOperator());
            if (inv == null) {
                return Result.error(400, "申请失败：账单不存在，或同账单已有未作废发票（幂等拒绝）");
            }
            return Result.ok().put("data", inv);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @Operation(summary = "开具（自编发票号）")
    @PostMapping("/{id}/issue")
    public Result issue(@PathVariable("id") Long id) {
        Result deny = denyIfNoIdentity();
        if (deny != null) {
            return deny;
        }
        Invoice inv = invoiceService.issue(id, currentOperator());
        if (inv == null) {
            return Result.error(400, "开具失败：发票不存在或状态非待申请/已申请");
        }
        return Result.ok().put("data", inv);
    }

    @Operation(summary = "邮寄（3→4）")
    @PostMapping("/{id}/mail")
    public Result mail(@PathVariable("id") Long id) {
        Result deny = denyIfNoIdentity();
        if (deny != null) {
            return deny;
        }
        boolean ok = invoiceService.mail(id, currentOperator());
        return ok ? Result.ok() : Result.error(400, "邮寄失败：发票不存在或状态非已开具");
    }

    @Operation(summary = "作废（→5，原因必填）")
    @PostMapping("/{id}/void")
    public Result voidInvoice(@PathVariable("id") Long id, @RequestParam("reason") String reason) {
        Result deny = denyIfNoIdentity();
        if (deny != null) {
            return deny;
        }
        try {
            boolean ok = invoiceService.voidInvoice(id, reason, currentOperator());
            return ok ? Result.ok() : Result.error(400, "作废失败：发票不存在或已作废");
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @Operation(summary = "分页查询")
    @GetMapping("/page")
    public Result page(@RequestParam(value = "page", required = false) Integer page,
                       @RequestParam(value = "pageSize", required = false) Integer pageSize,
                       @RequestParam(value = "customerId", required = false) String customerId,
                       @RequestParam(value = "status", required = false) Integer status) {
        Result deny = denyIfNoIdentity();
        if (deny != null) {
            return deny;
        }
        Page<Invoice> p = invoiceService.page(page, pageSize, customerId, status);
        return Result.ok().put("data", p);
    }
}
