package com.xinfei.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinfei.common.context.RequestContext;
import com.xinfei.common.utils.Result;
import com.xinfei.entity.TaskCost;
import com.xinfei.service.ITaskCostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 趟次成本内部接口 —— P0-6d/OPS-4 §6.3 最小集（录入/趟明细合计/分页）
 *
 * <p>数据/服务层，网关无 /api/work 路由；对外由 xfwl-web-manager 聚合
 * {@code /api/manager/cost/*} 暴露（见 CostFeign）。加油油耗/看板汇总属 P1。</p>
 */
@Slf4j
@RestController
@RequestMapping("cost")
@Tag(name = "趟次成本")
public class CostController {

    @Autowired
    private ITaskCostService taskCostService;

    private Result denyIfNoIdentity() {
        if (StringUtils.isBlank(RequestContext.getUserId())) {
            return Result.error(401, "未登录或身份信息缺失");
        }
        return null;
    }

    @Operation(summary = "录入成本")
    @PostMapping("")
    public Result save(@RequestParam("taskTransportId") Long taskTransportId,
                       @RequestParam(value = "truckId", required = false) Long truckId,
                       @RequestParam(value = "driverId", required = false) Long driverId,
                       @RequestParam("costType") String costType,
                       @RequestParam("amount") java.math.BigDecimal amount,
                       @RequestParam("occurTime") @org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") java.time.LocalDateTime occurTime,
                       @RequestParam(value = "voucher", required = false) String voucher,
                       @RequestParam(value = "remark", required = false) String remark) {
        Result deny = denyIfNoIdentity();
        if (deny != null) {
            return deny;
        }
        try {
            TaskCost cost = new TaskCost();
            cost.setTaskTransportId(taskTransportId);
            cost.setTruckId(truckId);
            cost.setDriverId(driverId);
            cost.setCostType(costType);
            cost.setAmount(amount);
            cost.setOccurTime(occurTime);
            cost.setVoucher(voucher);
            cost.setRemark(remark);
            if (StringUtils.isBlank(cost.getOperator())) {
                cost.setOperator(RequestContext.getUserId());
            }
            return Result.ok().put("data", taskCostService.saveCost(cost));
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @Operation(summary = "某趟成本明细+合计")
    @GetMapping("/task/{taskId}")
    public Result taskDetail(@PathVariable("taskId") Long taskId) {
        Result deny = denyIfNoIdentity();
        if (deny != null) {
            return deny;
        }
        Map<String, Object> vo = taskCostService.taskCostDetail(taskId);
        return Result.ok().put("data", vo);
    }

    @Operation(summary = "成本分页")
    @PostMapping("/page")
    public Result page(@RequestParam(value = "page", required = false) Integer page,
                       @RequestParam(value = "pageSize", required = false) Integer pageSize,
                       @RequestParam(value = "taskTransportId", required = false) Long taskTransportId,
                       @RequestParam(value = "truckId", required = false) Long truckId,
                       @RequestParam(value = "costType", required = false) String costType) {
        Result deny = denyIfNoIdentity();
        if (deny != null) {
            return deny;
        }
        Page<TaskCost> p = taskCostService.page(page, pageSize, taskTransportId, truckId, costType);
        return Result.ok().put("data", p);
    }
}
