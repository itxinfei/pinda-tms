package com.xinfei.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xinfei.common.utils.Result;
import com.xinfei.feign.CostFeign;
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
 * 管理端趟次成本—— 薄转发至 xfwl-work，网关前缀 /api/manager/cost。
 *
 * <p>P0-6d/OPS-4 的网关暴露层。权限码 {@code finance:cost:*}。</p>
 */
@RestController
@RequestMapping("cost")
@RequiredArgsConstructor
@Tag(name = "趟次成本")
public class CostController {

    private final CostFeign costFeign;

    @Operation(summary = "录入成本")
    @PostMapping("")
    @SaCheckPermission("finance:cost:*")
    public Result save(@RequestParam("taskTransportId") Long taskTransportId,
                       @RequestParam(value = "truckId", required = false) Long truckId,
                       @RequestParam(value = "driverId", required = false) Long driverId,
                       @RequestParam("costType") String costType,
                       @RequestParam("amount") java.math.BigDecimal amount,
                       @RequestParam("occurTime") String occurTime,
                       @RequestParam(value = "voucher", required = false) String voucher,
                       @RequestParam(value = "remark", required = false) String remark) {
        return costFeign.save(taskTransportId, truckId, driverId, costType, amount, occurTime, voucher, remark);
    }

    @Operation(summary = "某趟成本明细+合计")
    @GetMapping("/task/{taskId}")
    @SaCheckPermission("finance:cost:*")
    public Result taskDetail(@PathVariable("taskId") Long taskId) {
        return costFeign.taskDetail(taskId);
    }

    @Operation(summary = "成本分页")
    @PostMapping("/page")
    @SaCheckPermission("finance:cost:*")
    public Result page(@RequestParam(value = "page", required = false) Integer page,
                       @RequestParam(value = "pageSize", required = false) Integer pageSize,
                       @RequestParam(value = "taskTransportId", required = false) Long taskTransportId,
                       @RequestParam(value = "truckId", required = false) Long truckId,
                       @RequestParam(value = "costType", required = false) String costType) {
        return costFeign.page(page, pageSize, taskTransportId, truckId, costType);
    }
}
