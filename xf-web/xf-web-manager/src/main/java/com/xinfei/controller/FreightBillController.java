package com.xinfei.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xinfei.common.utils.Result;
import com.xinfei.feign.FreightBillFeign;
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
 * 管理端运费账单（对账）—— 薄转发至 xf-oms，网关前缀 /api/manager/freight-bill。
 *
 * <p>P0-6b 应收对账闭环的网关暴露层（内部服务 xf-oms 网关无路由，须由此聚合）。
 * 权限码 {@code finance:freight-bill:*}（据《21》§1.5，按《62》模型落 sys_menu 按钮行）。</p>
 */
@RestController
@RequestMapping("freight-bill")
@RequiredArgsConstructor
@Tag(name = "运费账单(对账)")
public class FreightBillController {

    private final FreightBillFeign freightBillFeign;

    @Operation(summary = "生成账单（归集应收结算单）")
    @PostMapping("/generate")
    @SaCheckPermission("finance:freight-bill:*")
    public Result generate(@RequestParam("customerId") String customerId,
                           @RequestParam("periodStart") String periodStart,
                           @RequestParam("periodEnd") String periodEnd) {
        return freightBillFeign.generate(customerId, periodStart, periodEnd);
    }

    @Operation(summary = "对账视图（账单头+结算单列表+差异标记）")
    @GetMapping("/{id}")
    @SaCheckPermission("finance:freight-bill:*")
    public Result reconcile(@PathVariable("id") Long id) {
        return freightBillFeign.reconcile(id);
    }

    @Operation(summary = "确认账单（0→1，结算单联动）")
    @PostMapping("/{id}/confirm")
    @SaCheckPermission("finance:freight-bill:*")
    public Result confirm(@PathVariable("id") Long id) {
        return freightBillFeign.confirm(id);
    }

    @Operation(summary = "核销账单（1→2，结算单联动）")
    @PostMapping("/{id}/write-off")
    @SaCheckPermission("finance:freight-bill:*")
    public Result writeOff(@PathVariable("id") Long id) {
        return freightBillFeign.writeOff(id);
    }
}
