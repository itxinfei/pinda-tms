package com.xinfei.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xinfei.DTO.WaybillMergeDTO;
import com.xinfei.common.utils.Result;
import com.xinfei.feign.TransportOrderFeign;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端运单拆合单（ORD-05）—— 薄转发至 xf-work，网关前缀 /api/manager/waybill-split-merge。
 *
 * <p>运单数据所有权在 xf-work，网关无 /api/work 路由，须由此聚合暴露。
 * 权限码 {@code transport:merge} / {@code transport:split}
 * （sys_menu type=B 按钮行，ADMIN 放行 *）。</p>
 */
@RestController
@RequestMapping("waybill-split-merge")
@RequiredArgsConstructor
@Tag(name = "运单拆合单(ORD-05)")
public class WaybillSplitMergeController {

    private final TransportOrderFeign transportOrderFeign;

    @Operation(summary = "合单（多个待调度运单归入主单）")
    @PostMapping("/merge")
    @SaCheckPermission("transport:merge")
    public Result merge(@RequestBody WaybillMergeDTO mergeDTO) {
        return transportOrderFeign.mergeWaybills(mergeDTO);
    }

    @Operation(summary = "拆单（解除合并组，全部还原 normal）")
    @PostMapping("/{mainId}/split")
    @SaCheckPermission("transport:split")
    public Result split(@PathVariable("mainId") Long mainId) {
        return transportOrderFeign.splitWaybills(mainId);
    }
}
