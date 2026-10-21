package com.xinfei.controller;

import com.xinfei.common.context.RequestContext;
import com.xinfei.common.utils.Result;
import com.xinfei.service.IProfitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 利润查询内部接口 —— P0-6d / FIN-4（V1 只做毛利，不建利润表）
 *
 * <p>数据/服务层；对外由 xfwl-web-manager 聚合 {@code /api/manager/profit/*}。
 * /profit/drill 钻取为 P1，本卡不做。</p>
 */
@Slf4j
@RestController
@RequestMapping("profit")
@Tag(name = "利润核算")
public class ProfitController {

    @Autowired
    private IProfitService profitService;

    private Result denyIfNoIdentity() {
        if (StringUtils.isBlank(RequestContext.getUserId())) {
            return Result.error(401, "未登录或身份信息缺失");
        }
        return null;
    }

    @Operation(summary = "单趟利润")
    @GetMapping("/task/{taskId}")
    public Result taskProfit(@PathVariable("taskId") Long taskId) {
        Result deny = denyIfNoIdentity();
        if (deny != null) {
            return deny;
        }
        return Result.ok().put("data", profitService.taskProfit(taskId));
    }

    @Operation(summary = "单订单利润(V1 均摊)")
    @GetMapping("/order/{orderId}")
    public Result orderProfit(@PathVariable("orderId") Long orderId) {
        Result deny = denyIfNoIdentity();
        if (deny != null) {
            return deny;
        }
        return Result.ok().put("data", profitService.orderProfit(orderId));
    }

    @Operation(summary = "利润汇总(按月)")
    @GetMapping("/summary")
    public Result summary() {
        Result deny = denyIfNoIdentity();
        if (deny != null) {
            return deny;
        }
        List<Map<String, Object>> data = profitService.summary();
        return Result.ok().put("data", data);
    }

    @Operation(summary = "利润排行(最赚/最亏)")
    @GetMapping("/ranking")
    public Result ranking() {
        Result deny = denyIfNoIdentity();
        if (deny != null) {
            return deny;
        }
        return Result.ok().put("data", profitService.ranking());
    }
}
