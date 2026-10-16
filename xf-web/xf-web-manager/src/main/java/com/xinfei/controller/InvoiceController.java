package com.xinfei.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xinfei.common.utils.Result;
import com.xinfei.feign.InvoiceFeign;
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
 * 管理端电子发票（开票）—— 薄转发至 xf-oms，网关前缀 /api/manager/invoice。
 *
 * <p>P0-6c 的网关暴露层（内部服务 xf-oms 网关无路由，须由此聚合）。不接真实税控。
 * 权限码 {@code finance:invoice:*}。</p>
 */
@RestController
@RequestMapping("invoice")
@RequiredArgsConstructor
@Tag(name = "电子发票(开票)")
public class InvoiceController {

    private final InvoiceFeign invoiceFeign;

    @Operation(summary = "申请开票（按账单）")
    @PostMapping("/apply")
    @SaCheckPermission("finance:invoice:*")
    public Result apply(@RequestParam("billId") Long billId,
                        @RequestParam(value = "type", required = false) Integer type,
                        @RequestParam(value = "taxNo", required = false) String taxNo,
                        @RequestParam(value = "mailAddress", required = false) String mailAddress) {
        return invoiceFeign.apply(billId, type, taxNo, mailAddress);
    }

    @Operation(summary = "开具（自编发票号）")
    @PostMapping("/{id}/issue")
    @SaCheckPermission("finance:invoice:*")
    public Result issue(@PathVariable("id") Long id) {
        return invoiceFeign.issue(id);
    }

    @Operation(summary = "邮寄（3→4）")
    @PostMapping("/{id}/mail")
    @SaCheckPermission("finance:invoice:*")
    public Result mail(@PathVariable("id") Long id) {
        return invoiceFeign.mail(id);
    }

    @Operation(summary = "作废（→5，原因必填）")
    @PostMapping("/{id}/void")
    @SaCheckPermission("finance:invoice:*")
    public Result voidInvoice(@PathVariable("id") Long id, @RequestParam("reason") String reason) {
        return invoiceFeign.voidInvoice(id, reason);
    }

    @Operation(summary = "分页查询")
    @GetMapping("/page")
    @SaCheckPermission("finance:invoice:*")
    public Result page(@RequestParam(value = "page", required = false) Integer page,
                       @RequestParam(value = "pageSize", required = false) Integer pageSize,
                       @RequestParam(value = "customerId", required = false) String customerId,
                       @RequestParam(value = "status", required = false) Integer status) {
        return invoiceFeign.page(page, pageSize, customerId, status);
    }
}
