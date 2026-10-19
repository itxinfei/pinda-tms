package com.xinfei.controller.carrier;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xinfei.common.context.RequestContext;
import com.xinfei.common.utils.Result;
import com.xinfei.controller.support.IdConverter;
import com.xinfei.entity.carrier.XfCarrier;
import com.xinfei.entity.carrier.XfCarrierDriver;
import com.xinfei.entity.carrier.XfCarrierTruck;
import com.xinfei.service.carrier.IPdCarrierDriverService;
import com.xinfei.service.carrier.IPdCarrierService;
import com.xinfei.service.carrier.IPdCarrierTruckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 承运商/外协运力内部接口 —— DSP-2 第一期（CAP-01）
 *
 * <p>数据/服务层，网关无 /api/base 路由；对外由 xf-web-manager 聚合
 * {@code /api/manager/carrier/*} 暴露（见 CarrierFeign）。</p>
 */
@Slf4j
@RestController
@RequestMapping("carrier")
@Tag(name = "承运商/外协运力池")
public class CarrierController {

    @Autowired
    private IPdCarrierService carrierService;
    @Autowired
    private IPdCarrierTruckService carrierTruckService;
    @Autowired
    private IPdCarrierDriverService carrierDriverService;

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

    @Operation(summary = "新增/修改承运商")
    @PostMapping
    public Result save(@RequestBody XfCarrier carrier) {
        Result deny = denyIfNoIdentity();
        if (deny != null) {
            return deny;
        }
        try {
            XfCarrier saved = carrierService.saveCarrier(carrier, currentOperator());
            return Result.ok().put("data", saved);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @Operation(summary = "分页查询")
    @GetMapping("/page")
    public Result page(@RequestParam(value = "page", required = false) Integer page,
                       @RequestParam(value = "pageSize", required = false) Integer pageSize,
                       @RequestParam(value = "code", required = false) String code,
                       @RequestParam(value = "name", required = false) String name,
                       @RequestParam(value = "status", required = false) Integer status) {
        Result deny = denyIfNoIdentity();
        if (deny != null) {
            return deny;
        }
        IPage<XfCarrier> p = carrierService.page(page, pageSize, code, name, status);
        return Result.ok().put("data", p);
    }

    @Operation(summary = "详情（含挂靠车辆/司机）")
    @GetMapping("/{id}")
    public Result detail(@PathVariable("id") Long id) {
        Result deny = denyIfNoIdentity();
        if (deny != null) {
            return deny;
        }
        XfCarrier carrier = carrierService.getById(id);
        if (carrier == null) {
            return Result.error(404, "承运商不存在");
        }
        List<XfCarrierTruck> trucks = carrierTruckService.listByCarrierId(id);
        List<XfCarrierDriver> drivers = carrierDriverService.listByCarrierId(id);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("carrier", carrier);
        data.put("trucks", trucks);
        data.put("drivers", drivers);
        return Result.ok().put("data", data);
    }

    @Operation(summary = "状态流转（1合作中/0暂停/2黑名单）")
    @PostMapping("/{id}/status")
    public Result changeStatus(@PathVariable("id") Long id,
                               @RequestParam("status") Integer status) {
        Result deny = denyIfNoIdentity();
        if (deny != null) {
            return deny;
        }
        try {
            boolean ok = carrierService.changeStatus(id, status, currentOperator());
            return ok ? Result.ok() : Result.error(400, "状态变更失败");
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @Operation(summary = "挂靠车辆新增/修改")
    @PostMapping("/truck")
    public Result saveTruck(@RequestBody XfCarrierTruck truck) {
        Result deny = denyIfNoIdentity();
        if (deny != null) {
            return deny;
        }
        try {
            return Result.ok().put("data", carrierTruckService.saveTruck(truck, currentOperator()));
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @Operation(summary = "挂靠司机新增/修改")
    @PostMapping("/driver")
    public Result saveDriver(@RequestBody XfCarrierDriver driver) {
        Result deny = denyIfNoIdentity();
        if (deny != null) {
            return deny;
        }
        try {
            return Result.ok().put("data", carrierDriverService.saveDriver(driver, currentOperator()));
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @Operation(summary = "运力池查询（合作中承运商的正常车辆）")
    @GetMapping("/capacity")
    public Result capacity(@RequestParam(value = "keyword", required = false) String keyword,
                           @RequestParam(value = "truckTypeId", required = false) Long truckTypeId,
                           @RequestParam(value = "minLoad", required = false) BigDecimal minLoad) {
        Result deny = denyIfNoIdentity();
        if (deny != null) {
            return deny;
        }
        return Result.ok().put("data", carrierService.capacity(keyword, truckTypeId, minLoad));
    }
}
