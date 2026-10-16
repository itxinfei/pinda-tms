package com.xinfei.controller;

import com.xinfei.DTO.WaybillMergeDTO;
import com.xinfei.common.context.RequestContext;
import com.xinfei.common.utils.Result;
import com.xinfei.service.IWaybillSplitMergeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 运单拆合单（ORD-05）内部接口
 *
 * <p>数据所有权在 xf-work（work_transport_order），订单联动校验走 OrderFeign。
 * 对外由 xf-web-manager 聚合 {@code /api/manager/waybill-split-merge/*} 暴露。</p>
 */
@Slf4j
@RestController
@RequestMapping("waybill-split-merge")
@Tag(name = "运单拆合单(ORD-05)")
public class WaybillSplitMergeController {

    @Autowired
    private IWaybillSplitMergeService waybillSplitMergeService;

    private Result denyIfNoIdentity() {
        if (StringUtils.isBlank(RequestContext.getUserId())) {
            return Result.error(401, "未登录或身份信息缺失");
        }
        return null;
    }

    @Operation(summary = "合单（多个待调度运单归入主单）")
    @PostMapping("/merge")
    public Result merge(@RequestBody WaybillMergeDTO mergeDTO) {
        Result deny = denyIfNoIdentity();
        if (deny != null) {
            return deny;
        }
        if (mergeDTO == null) {
            return Result.error(400, "合单入参不能为空");
        }
        try {
            waybillSplitMergeService.merge(mergeDTO.getMainId(), mergeDTO.getChildIds(), RequestContext.getUserName());
            return Result.ok();
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @Operation(summary = "拆单（解除合并组，全部还原 normal）")
    @PostMapping("/{mainId}/split")
    public Result split(@PathVariable("mainId") Long mainId) {
        Result deny = denyIfNoIdentity();
        if (deny != null) {
            return deny;
        }
        try {
            waybillSplitMergeService.split(mainId, RequestContext.getUserName());
            return Result.ok();
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }
}
