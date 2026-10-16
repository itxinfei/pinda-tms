package com.xinfei.controller;

import com.xinfei.common.context.RequestContext;
import com.xinfei.common.utils.IdConverter;
import com.xinfei.common.utils.Result;
import com.xinfei.entity.FreightBill;
import com.xinfei.service.IFreightBillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * 运费账单（对账）内部接口 —— P0-6b 应收对账闭环
 *
 * <p>数据/服务层，网关无 /api/oms 路由；对外由 xf-web-manager 聚合
 * {@code /api/manager/freight-bill/*} 暴露（见 FreightBillFeign）。</p>
 *
 * <p>身份校验 deny-by-default：直连端口须网关透传 userid，否则拒绝。</p>
 */
@Slf4j
@RestController
@RequestMapping("freight-bill")
@Tag(name = "运费账单(对账)")
public class FreightBillController {

    @Autowired
    private IFreightBillService freightBillService;

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

    @Operation(summary = "生成账单（归集应收结算单）")
    @PostMapping("/generate")
    public Result generate(@RequestParam("customerId") String customerId,
                           @RequestParam("periodStart") LocalDate periodStart,
                           @RequestParam("periodEnd") LocalDate periodEnd) {
        Result deny = denyIfNoIdentity();
        if (deny != null) {
            return deny;
        }
        if (StringUtils.isBlank(customerId) || periodStart == null || periodEnd == null) {
            return Result.error(400, "客户id与账期起止不能为空");
        }
        try {
            FreightBill bill = freightBillService.generate(customerId, periodStart, periodEnd, currentOperator());
            if (bill == null) {
                return Result.error(400, "无可归集的应收结算单，或同客户同账期账单已存在（幂等拒绝）");
            }
            return Result.ok().put("data", bill);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @Operation(summary = "对账视图（账单头+结算单列表+差异标记）")
    @GetMapping("/{id}")
    public Result reconcile(@PathVariable("id") Long id) {
        Result deny = denyIfNoIdentity();
        if (deny != null) {
            return deny;
        }
        Map<String, Object> vo = freightBillService.reconcile(id);
        if (vo == null) {
            return Result.error(404, "账单不存在");
        }
        return Result.ok().put("data", vo);
    }

    @Operation(summary = "确认账单（0→1，结算单联动 0→1）")
    @PostMapping("/{id}/confirm")
    public Result confirm(@PathVariable("id") Long id) {
        Result deny = denyIfNoIdentity();
        if (deny != null) {
            return deny;
        }
        boolean ok = freightBillService.confirm(id, currentOperator());
        return ok ? Result.ok() : Result.error(400, "确认失败：账单不存在或状态非待确认");
    }

    @Operation(summary = "核销账单（1→2，结算单联动 1→2）")
    @PostMapping("/{id}/write-off")
    public Result writeOff(@PathVariable("id") Long id) {
        Result deny = denyIfNoIdentity();
        if (deny != null) {
            return deny;
        }
        boolean ok = freightBillService.writeOff(id, currentOperator());
        return ok ? Result.ok() : Result.error(400, "核销失败：账单不存在或状态非已确认");
    }
}
