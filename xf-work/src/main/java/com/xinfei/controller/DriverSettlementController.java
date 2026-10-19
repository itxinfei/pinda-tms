package com.xinfei.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinfei.common.context.RequestContext;
import com.xinfei.common.utils.Result;
import com.xinfei.entity.DriverSettlement;
import com.xinfei.service.IDriverSettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 司机趟次结算内部接口 —— FIN-2（《21》§FIN-2 第一期定稿）
 *
 * <p>数据/服务层，网关无 /api/work 路由；对外由 xf-web-manager 聚合
 * {@code /api/manager/driver-settlement/*} 暴露（见 DriverSettlementFeign）。
 * 账期入参 yyyy-MM-dd 字符串绑 LocalDate。</p>
 */
@Slf4j
@RestController
@RequestMapping("driver-settlement")
@RequiredArgsConstructor
@Tag(name = "司机趟次结算")
public class DriverSettlementController {

    private final IDriverSettlementService driverSettlementService;

    private Result denyIfNoIdentity() {
        if (StringUtils.isBlank(RequestContext.getUserId())) {
            return Result.error(401, "未登录或身份信息缺失");
        }
        return null;
    }

    /** RequestContext.userId(String) → Long（工号外场景通常为数字，解析失败视为空） */
    private Long operatorId() {
        try {
            return Long.valueOf(RequestContext.getUserId());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Operation(summary = "按司机+账期生成结算单")
    @PostMapping("")
    public Result generate(@RequestParam("driverId") Long driverId,
                           @RequestParam("periodStart") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate periodStart,
                           @RequestParam("periodEnd") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate periodEnd,
                           @RequestParam(value = "baseWage", required = false) BigDecimal baseWage,
                           @RequestParam(value = "allowance", required = false) BigDecimal allowance,
                           @RequestParam(value = "deduction", required = false) BigDecimal deduction,
                           @RequestParam(value = "remark", required = false) String remark) {
        Result deny = denyIfNoIdentity();
        if (deny != null) {
            return deny;
        }
        try {
            DriverSettlement s = driverSettlementService.generate(driverId, periodStart, periodEnd,
                    baseWage, allowance, deduction, remark, operatorId());
            return Result.ok().put("data", s);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @Operation(summary = "候选司机（账期内有可归集趟次）")
    @GetMapping("/drivers")
    public Result drivers(@RequestParam("periodStart") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate periodStart,
                          @RequestParam("periodEnd") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate periodEnd) {
        Result deny = denyIfNoIdentity();
        if (deny != null) {
            return deny;
        }
        try {
            List<Map<String, Object>> list = driverSettlementService.candidateDrivers(periodStart, periodEnd);
            return Result.ok().put("data", list);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @Operation(summary = "结算单分页")
    @PostMapping("/page")
    public Result page(@RequestParam(value = "page", required = false) Integer page,
                       @RequestParam(value = "pageSize", required = false) Integer pageSize,
                       @RequestParam(value = "settleNo", required = false) String settleNo,
                       @RequestParam(value = "driverId", required = false) Long driverId,
                       @RequestParam(value = "status", required = false) Integer status) {
        Result deny = denyIfNoIdentity();
        if (deny != null) {
            return deny;
        }
        Page<DriverSettlement> p = driverSettlementService.page(page, pageSize, settleNo, driverId, status);
        return Result.ok().put("data", p);
    }

    @Operation(summary = "详情（头+逐趟明细）")
    @GetMapping("/{id}")
    public Result detail(@PathVariable("id") Long id) {
        Result deny = denyIfNoIdentity();
        if (deny != null) {
            return deny;
        }
        try {
            Map<String, Object> vo = driverSettlementService.detail(id);
            return Result.ok().put("data", vo);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @Operation(summary = "审核（0→1，可带审核备注）")
    @PostMapping("/{id}/audit")
    public Result audit(@PathVariable("id") Long id,
                        @RequestParam(value = "auditRemark", required = false) String auditRemark) {
        Result deny = denyIfNoIdentity();
        if (deny != null) {
            return deny;
        }
        boolean ok = driverSettlementService.audit(id, auditRemark, operatorId());
        return ok ? Result.ok() : Result.error(400, "结算单状态已变化，请刷新后重试");
    }

    @Operation(summary = "发放登记（1→2，线下打款后留痕）")
    @PostMapping("/{id}/pay")
    public Result pay(@PathVariable("id") Long id) {
        Result deny = denyIfNoIdentity();
        if (deny != null) {
            return deny;
        }
        boolean ok = driverSettlementService.pay(id, operatorId());
        return ok ? Result.ok() : Result.error(400, "结算单状态已变化，请刷新后重试");
    }
}
