package com.xinfei.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinfei.common.context.RequestContext;
import com.xinfei.common.utils.IdConverter;
import com.xinfei.common.utils.Result;
import com.xinfei.entity.FeeItem;
import com.xinfei.service.IFeeItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 附加费费用项配置内部接口 —— POD-03 第一期 / FA-10
 *
 * <p>数据/服务层，网关无 /api/oms 路由；对外由 xf-web-manager 聚合
 * {@code /api/manager/fee-item/*} 暴露（见 FeeItemFeign）。</p>
 */
@Slf4j
@RestController
@RequestMapping("fee-item")
@Tag(name = "附加费费用项配置")
public class FeeItemController {

    @Autowired
    private IFeeItemService feeItemService;

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

    @Operation(summary = "新增/修改费用项")
    @PostMapping
    public Result save(@RequestBody FeeItem item) {
        Result deny = denyIfNoIdentity();
        if (deny != null) {
            return deny;
        }
        try {
            FeeItem saved = feeItemService.saveItem(item, currentOperator());
            return Result.ok().put("data", saved);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @Operation(summary = "停用费用项")
    @PostMapping("/{id}/disable")
    public Result disable(@PathVariable("id") Long id) {
        Result deny = denyIfNoIdentity();
        if (deny != null) {
            return deny;
        }
        boolean ok = feeItemService.disable(id, currentOperator());
        return ok ? Result.ok() : Result.error(400, "停用失败：费用项不存在或已停用");
    }

    @Operation(summary = "分页查询")
    @GetMapping("/page")
    public Result page(@RequestParam(value = "page", required = false) Integer page,
                       @RequestParam(value = "pageSize", required = false) Integer pageSize,
                       @RequestParam(value = "feeCode", required = false) String feeCode,
                       @RequestParam(value = "status", required = false) Integer status) {
        Result deny = denyIfNoIdentity();
        if (deny != null) {
            return deny;
        }
        Page<FeeItem> p = feeItemService.page(page, pageSize, feeCode, status);
        return Result.ok().put("data", p);
    }
}
