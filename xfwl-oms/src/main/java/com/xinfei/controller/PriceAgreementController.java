package com.xinfei.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinfei.common.context.RequestContext;
import com.xinfei.common.utils.IdConverter;
import com.xinfei.common.utils.Result;
import com.xinfei.entity.PriceAgreement;
import com.xinfei.service.IPriceAgreementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 客户价格协议内部接口 —— FIN-1.1 / POD-06 客户侧费率卡
 *
 * <p>数据/服务层，网关无 /api/oms 路由；对外由 xfwl-web-manager 聚合
 * {@code /api/manager/price-agreement/*} 暴露（见 PriceAgreementFeign）。</p>
 */
@Slf4j
@RestController
@RequestMapping("price-agreement")
@Tag(name = "客户价格协议(费率卡)")
public class PriceAgreementController {

    @Autowired
    private IPriceAgreementService priceAgreementService;

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

    @Operation(summary = "新增/修改协议")
    @PostMapping
    public Result save(@RequestBody PriceAgreement agreement) {
        Result deny = denyIfNoIdentity();
        if (deny != null) {
            return deny;
        }
        try {
            PriceAgreement saved = priceAgreementService.saveAgreement(agreement, currentOperator());
            return Result.ok().put("data", saved);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @Operation(summary = "停用协议")
    @PostMapping("/{id}/disable")
    public Result disable(@PathVariable("id") Long id) {
        Result deny = denyIfNoIdentity();
        if (deny != null) {
            return deny;
        }
        boolean ok = priceAgreementService.disable(id, currentOperator());
        return ok ? Result.ok() : Result.error(400, "停用失败：协议不存在或已停用");
    }

    @Operation(summary = "分页查询")
    @GetMapping("/page")
    public Result page(@RequestParam(value = "page", required = false) Integer page,
                       @RequestParam(value = "pageSize", required = false) Integer pageSize,
                       @RequestParam(value = "memberId", required = false) String memberId,
                       @RequestParam(value = "status", required = false) Integer status) {
        Result deny = denyIfNoIdentity();
        if (deny != null) {
            return deny;
        }
        Page<PriceAgreement> p = priceAgreementService.page(page, pageSize, memberId, status);
        return Result.ok().put("data", p);
    }
}
