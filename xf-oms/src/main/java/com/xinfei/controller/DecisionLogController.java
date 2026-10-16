package com.xinfei.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinfei.common.context.RequestContext;
import com.xinfei.common.utils.IdConverter;
import com.xinfei.common.utils.Result;
import com.xinfei.entity.DecisionLog;
import com.xinfei.service.IDecisionLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 合规决策留痕内部接口 —— P0-9 / FR-75
 *
 * <p>数据/服务层；对外由 xf-web-manager 聚合 {@code /api/manager/decision-log/*}。
 * 改派(REASSIGN)埋点待 G2 派单改派落地时接入。</p>
 */
@Slf4j
@RestController
@RequestMapping("decision-log")
@Tag(name = "合规决策留痕")
public class DecisionLogController {

    @Autowired
    private IDecisionLogService decisionLogService;

    private Result denyIfNoIdentity() {
        if (StringUtils.isBlank(RequestContext.getUserId())) {
            return Result.error(401, "未登录或身份信息缺失");
        }
        return null;
    }

    @Operation(summary = "落留痕")
    @PostMapping("/apply")
    public Result apply(@RequestParam("businessType") String businessType,
                        @RequestParam("businessId") Long businessId,
                        @RequestParam("reasonEnum") String reasonEnum,
                        @RequestParam(value = "detail", required = false) String detail) {
        Result deny = denyIfNoIdentity();
        if (deny != null) {
            return deny;
        }
        try {
            Long operator = IdConverter.toLong(RequestContext.getUserId());
            return Result.ok().put("data", decisionLogService.apply(businessType, businessId, reasonEnum, detail, operator));
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @Operation(summary = "分页查询")
    @GetMapping("/page")
    public Result page(@RequestParam(value = "page", required = false) Integer page,
                       @RequestParam(value = "pageSize", required = false) Integer pageSize,
                       @RequestParam(value = "businessType", required = false) String businessType,
                       @RequestParam(value = "businessId", required = false) Long businessId) {
        Result deny = denyIfNoIdentity();
        if (deny != null) {
            return deny;
        }
        Page<DecisionLog> p = decisionLogService.page(page, pageSize, businessType, businessId);
        return Result.ok().put("data", p);
    }

    @Operation(summary = "监管导出")
    @GetMapping("/export")
    public Result export(@RequestParam(value = "businessType", required = false) String businessType,
                         @RequestParam(value = "businessId", required = false) Long businessId,
                         @RequestParam(value = "beginTime", required = false) String beginTime,
                         @RequestParam(value = "endTime", required = false) String endTime) {
        Result deny = denyIfNoIdentity();
        if (deny != null) {
            return deny;
        }
        List<Map<String, Object>> rows = decisionLogService.exportRows(businessType, businessId, beginTime, endTime);
        return Result.ok().put("data", rows);
    }
}
