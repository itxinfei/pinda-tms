package com.xinfei.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xinfei.DTO.UserDTO;
import com.xinfei.DTO.user.TruckDriverDto;
import com.xinfei.common.utils.Result;
import com.xinfei.feign.UserFeign;
import com.xinfei.feign.user.DriverFeign;
import com.xinfei.feign.DriverSettlementFeign;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理端司机趟次结算 —— 薄转发至 xfwl-work，网关前缀 /api/manager/driver-settlement。
 *
 * <p>FIN-2（《21》§FIN-2 第一期定稿 2026-10-20）。内部服务 xfwl-work 网关无路由，
 * 由此聚合暴露；司机名经 DriverFeign（base_truck_driver→user_id）+ UserFeign（sys_user.name）
 * 两跳装配（兼容 driver_id 直接落 sys_user id 的存量数据，如 mock），兜底 "司机#id"。
 * 权限码 {@code finance:driver-settlement:*} 具体码禁通配。</p>
 */
@Slf4j
@RestController
@RequestMapping("driver-settlement")
@RequiredArgsConstructor
@Tag(name = "司机趟次结算")
public class DriverSettlementController {

    private final DriverSettlementFeign driverSettlementFeign;
    private final DriverFeign driverFeign;
    private final UserFeign userFeign;

    @Operation(summary = "按司机+账期生成结算单")
    @PostMapping("/generate")
    @SaCheckPermission("finance:driver-settlement:generate")
    public Result generate(@RequestParam("driverId") Long driverId,
                           @RequestParam("periodStart") String periodStart,
                           @RequestParam("periodEnd") String periodEnd,
                           @RequestParam(value = "baseWage", required = false) BigDecimal baseWage,
                           @RequestParam(value = "allowance", required = false) BigDecimal allowance,
                           @RequestParam(value = "deduction", required = false) BigDecimal deduction,
                           @RequestParam(value = "remark", required = false) String remark) {
        Result r = driverSettlementFeign.generate(driverId, periodStart, periodEnd,
                baseWage, allowance, deduction, remark);
        enrichSingle(r);
        return r;
    }

    @Operation(summary = "候选司机（账期内有可归集趟次）")
    @GetMapping("/drivers")
    @SaCheckPermission("finance:driver-settlement:drivers")
    public Result drivers(@RequestParam("periodStart") String periodStart,
                          @RequestParam("periodEnd") String periodEnd) {
        Result r = driverSettlementFeign.drivers(periodStart, periodEnd);
        enrichCandidates(r);
        return r;
    }

    @Operation(summary = "结算单分页")
    @PostMapping("/page")
    @SaCheckPermission("finance:driver-settlement:page")
    public Result page(@RequestParam(value = "page", required = false) Integer page,
                       @RequestParam(value = "pageSize", required = false) Integer pageSize,
                       @RequestParam(value = "settleNo", required = false) String settleNo,
                       @RequestParam(value = "driverId", required = false) Long driverId,
                       @RequestParam(value = "status", required = false) Integer status,
                       @RequestParam(value = "periodStart", required = false) String periodStart,
                       @RequestParam(value = "periodEnd", required = false) String periodEnd) {
        Result r = driverSettlementFeign.page(page, pageSize, settleNo, driverId, status, periodStart, periodEnd);
        enrichPage(r);
        return r;
    }

    @Operation(summary = "详情（头+逐趟明细）")
    @GetMapping("/{id}")
    @SaCheckPermission("finance:driver-settlement:detail")
    public Result detail(@PathVariable("id") Long id) {
        Result r = driverSettlementFeign.detail(id);
        enrichSingle(r);
        return r;
    }

    @Operation(summary = "审核（0→1）")
    @PostMapping("/{id}/audit")
    @SaCheckPermission("finance:driver-settlement:audit")
    public Result audit(@PathVariable("id") Long id,
                        @RequestParam(value = "auditRemark", required = false) String auditRemark) {
        return driverSettlementFeign.audit(id, auditRemark);
    }

    @Operation(summary = "发放登记（1→2，线下打款后留痕）")
    @PostMapping("/{id}/pay")
    @SaCheckPermission("finance:driver-settlement:pay")
    public Result pay(@PathVariable("id") Long id) {
        return driverSettlementFeign.pay(id);
    }

    // ---------- driverName 装配（JSON 级，避免引入 VO 复制） ----------

    /** page 结果：data 为 Page 序列化 Map，遍历 records 补 driverName */
    @SuppressWarnings("unchecked")
    private void enrichPage(Result r) {
        if (r == null || !(r.get("data") instanceof Map)) {
            return;
        }
        Object records = ((Map<String, Object>) r.get("data")).get("records");
        if (!(records instanceof List)) {
            return;
        }
        List<Long> ids = collectDriverIds((List<Object>) records, "driverId");
        Map<Long, String> names = resolveNames(ids);
        for (Object row : (List<Object>) records) {
            putName(row, names);
        }
    }

    /** detail 结果：data={settlement,details}，给 settlement 头补 driverName */
    private void enrichSingle(Result r) {
        if (r == null || !(r.get("data") instanceof Map)) {
            return;
        }
        Map<String, Object> data = (Map<String, Object>) r.get("data");
        if (data.get("settlement") instanceof Map) {
            Map<String, Object> settlement = (Map<String, Object>) data.get("settlement");
            List<Long> ids = new ArrayList<>();
            Long driverId = toLong(settlement.get("driverId"));
            if (driverId != null) {
                ids.add(driverId);
            }
            Map<Long, String> names = resolveNames(ids);
            settlement.put("driverName", names.getOrDefault(driverId, "司机#" + driverId));
        }
    }

    /** drivers 候选：data=List<{driverId,tripCount}>，补 driverName */
    private void enrichCandidates(Result r) {
        if (r == null || !(r.get("data") instanceof List)) {
            return;
        }
        List<Object> rows = (List<Object>) r.get("data");
        Map<Long, String> names = resolveNames(collectDriverIds(rows, "driverId"));
        for (Object row : rows) {
            putName(row, names);
        }
    }

    private List<Long> collectDriverIds(List<Object> rows, String key) {
        List<Long> ids = new ArrayList<>();
        for (Object row : rows) {
            if (row instanceof Map) {
                Long id = toLong(((Map<?, ?>) row).get(key));
                if (id != null && !ids.contains(id)) {
                    ids.add(id);
                }
            }
        }
        return ids;
    }

    private void putName(Object row, Map<Long, String> names) {
        if (!(row instanceof Map)) {
            return;
        }
        Map<?, ?> m = (Map<?, ?>) row;
        Long id = toLong(m.get("driverId"));
        if (id != null) {
            ((Map<String, Object>) row).put("driverName", names.getOrDefault(id, "司机#" + id));
        }
    }

    /**
     * 司机名两跳解析：base_truck_driver.id → user_id → sys_user.name；
     * 未匹配的 driverId 直接按 sys_user id 兜底查（兼容 mock 直落 sys_user id 的存量数据）。
     */
    private Map<Long, String> resolveNames(Collection<Long> driverIds) {
        Map<Long, String> result = new HashMap<>();
        if (driverIds == null || driverIds.isEmpty()) {
            return result;
        }
        List<String> idStrs = driverIds.stream().map(String::valueOf).toList();
        Map<Long, Long> driverToUser = new HashMap<>();
        try {
            List<TruckDriverDto> drivers = driverFeign.findAll(idStrs);
            if (drivers != null) {
                for (TruckDriverDto d : drivers) {
                    Long driverId = parseLong(d.getId());
                    Long userId = parseLong(d.getUserId());
                    if (driverId != null && userId != null) {
                        driverToUser.put(driverId, userId);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[司机结算] DriverFeign 司机查询失败，按 sys_user id 直查兜底: {}", e.getMessage());
        }
        List<Long> userIds = new ArrayList<>();
        for (Long id : driverIds) {
            Long uid = driverToUser.getOrDefault(id, id);
            if (!userIds.contains(uid)) {
                userIds.add(uid);
            }
        }
        Map<Long, String> userNameById = new HashMap<>();
        try {
            List<UserDTO> users = userFeign.list(userIds, null, null, null);
            if (users != null) {
                for (UserDTO u : users) {
                    if (u.getId() != null) {
                        userNameById.put(u.getId(), u.getName());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[司机结算] UserFeign 用户查询失败: {}", e.getMessage());
        }
        for (Long id : driverIds) {
            Long uid = driverToUser.getOrDefault(id, id);
            String name = userNameById.get(uid);
            result.put(id, name != null && !name.isBlank() ? name : "司机#" + id);
        }
        return result;
    }

    private Long parseLong(String v) {
        try {
            return v == null || v.isBlank() ? null : Long.valueOf(v);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long toLong(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number n) {
            return n.longValue();
        }
        return parseLong(String.valueOf(v));
    }
}
