package com.xinfei.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinfei.common.utils.Result;
import com.xinfei.controller.support.IdConverter;
import com.xinfei.entity.LocationRecord;
import com.xinfei.enums.BusinessType;
import com.xinfei.service.ILocationRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GPS轨迹查询接口
 *
 * <p>基于落库的 {@link LocationRecord}（iot_truck_location 表）提供：
 * 轨迹回放、最近位置、按运输任务批量查询、分页能力，供管理端车辆轨迹监控、
 * 客户小程序订单轨迹使用。</p>
 */
@Slf4j
@RestController
@RequestMapping("/trace")
@Tag(name = "车辆轨迹查询")
public class GpsTraceController {

    /** 单次按运输任务批量查询的上限 */
    private static final int MAX_TASK_QUERY_SIZE = 50;

    @Autowired
    private ILocationRecordService locationRecordService;

    /**
     * 轨迹回放：按业务ID（车辆/快递员）+ 类型查询完整轨迹（按上报时间升序）
     *
     * @param businessId 业务ID（车辆ID或快递员ID）
     * @param type       类型别名 truck/courier（可选）
     * @return 轨迹点列表
     */
    @Operation(summary = "轨迹回放")
    @GetMapping("/replay")
    public Result replay(@RequestParam("businessId") String businessId,
                         @RequestParam(value = "type", required = false) String type) {
        Long businessIdValue = IdConverter.toLong(businessId);
        if (businessIdValue == null) {
            return Result.error(400, "businessId不能为空且必须为数字");
        }
        LambdaQueryWrapper<LocationRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LocationRecord::getBusinessId, businessIdValue);
        Integer businessType = BusinessType.codeOfAlias(type);
        if (businessType != null) {
            wrapper.eq(LocationRecord::getBusinessType, businessType);
        }
        // 按上报时间升序，还原行驶轨迹
        wrapper.orderByAsc(LocationRecord::getReportTime);
        List<LocationRecord> records = locationRecordService.list(wrapper);
        return Result.ok().put("data", records);
    }

    /**
     * 最近位置：按业务ID + 类型查询最新一条位置
     *
     * @param businessId 业务ID
     * @param type       类型别名（可选）
     * @return 最新位置记录
     */
    @Operation(summary = "最近位置")
    @GetMapping("/latest")
    public Result latest(@RequestParam("businessId") String businessId,
                         @RequestParam(value = "type", required = false) String type) {
        Long businessIdValue = IdConverter.toLong(businessId);
        if (businessIdValue == null) {
            return Result.error(400, "businessId不能为空且必须为数字");
        }
        LambdaQueryWrapper<LocationRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LocationRecord::getBusinessId, businessIdValue);
        Integer businessType = BusinessType.codeOfAlias(type);
        if (businessType != null) {
            wrapper.eq(LocationRecord::getBusinessType, businessType);
        }
        wrapper.orderByDesc(LocationRecord::getReportTime);
        wrapper.last("limit 1");
        LocationRecord record = locationRecordService.getOne(wrapper);
        return Result.ok().put("data", record);
    }

    /**
     * 按运输任务ID批量查询轨迹（供客户小程序“我的订单→轨迹”使用）。
     *
     * <p>一个订单可能跨多个运输任务区段，用 transport_task_id IN(...) 一次取回，
     * 按任务、上报时间升序。</p>
     *
     * @param taskIds 运输任务ID列表，去空去重后最多 50 个
     * @return 轨迹点列表（可能为空，但不会报错）
     */
    @Operation(summary = "按运输任务批量查轨迹")
    @GetMapping("/byTasks")
    public Result byTasks(@RequestParam("taskIds") List<String> taskIds) {
        List<Long> ids = IdConverter.toLongList(taskIds);
        if (ids == null) {
            return Result.error(400, "taskIds不能为空");
        }
        // 去重（toLongList 已剔除非法值）
        List<Long> distinctIds = ids.stream().distinct().collect(java.util.stream.Collectors.toList());
        if (distinctIds.isEmpty()) {
            return Result.error(400, "taskIds不能为空");
        }
        if (distinctIds.size() > MAX_TASK_QUERY_SIZE) {
            return Result.error(400, "单次最多查询50个运输任务");
        }
        LambdaQueryWrapper<LocationRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(LocationRecord::getTransportTaskId, distinctIds);
        wrapper.orderByAsc(LocationRecord::getTransportTaskId);
        wrapper.orderByAsc(LocationRecord::getReportTime);
        List<LocationRecord> records = locationRecordService.list(wrapper);
        return Result.ok().put("data", records);
    }

    /**
     * 分页查询：按业务ID/类型/运输任务/车牌号筛选
     *
     * @param params page、pageSize、businessId、type、transportTaskId、licensePlate
     * @return 分页结果
     */
    @Operation(summary = "轨迹分页查询")
    @PostMapping("/page")
    public Result page(@RequestBody Map<String, Object> params) {
        // 分页参数防御性解析：非法或越界时返回 400，避免 500
        int pageNum;
        int pageSize;
        try {
            pageNum = params.get("page") == null ? 1 : Integer.parseInt(params.get("page").toString());
            pageSize = params.get("pageSize") == null ? 10 : Integer.parseInt(params.get("pageSize").toString());
        } catch (NumberFormatException e) {
            log.warn("[轨迹查询] 分页参数非法: {}", params);
            return Result.error(400, "分页参数必须为数字");
        }
        if (pageNum < 1 || pageSize < 1 || pageSize > 200) {
            log.warn("[轨迹查询] 分页参数越界: page={}, pageSize={}", pageNum, pageSize);
            return Result.error(400, "分页参数越界：page>=1，1<=pageSize<=200");
        }

        LambdaQueryWrapper<LocationRecord> wrapper = new LambdaQueryWrapper<>();
        Long businessId = IdConverter.toLong(asString(params.get("businessId")));
        if (businessId != null) {
            wrapper.eq(LocationRecord::getBusinessId, businessId);
        }
        Integer businessType = BusinessType.codeOfAlias(asString(params.get("type")));
        if (businessType != null) {
            wrapper.eq(LocationRecord::getBusinessType, businessType);
        }
        Long transportTaskId = IdConverter.toLong(asString(params.get("transportTaskId")));
        if (transportTaskId != null) {
            wrapper.eq(LocationRecord::getTransportTaskId, transportTaskId);
        }
        String licensePlate = asString(params.get("licensePlate"));
        if (StringUtils.isNotBlank(licensePlate)) {
            wrapper.like(LocationRecord::getLicensePlate, licensePlate);
        }
        wrapper.orderByDesc(LocationRecord::getCreateTime);

        IPage<LocationRecord> page = locationRecordService.page(new Page<>(pageNum, pageSize), wrapper);
        Map<String, Object> data = new HashMap<>();
        data.put("items", page.getRecords());
        data.put("total", page.getTotal());
        data.put("pages", page.getPages());
        data.put("page", pageNum);
        data.put("pageSize", pageSize);
        return Result.ok().put("data", data);
    }

    /**
     * 把分页参数对象安全转成去空白字符串，null 或空白返回 null
     */
    private String asString(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString();
        return StringUtils.isBlank(text) ? null : text.trim();
    }
}
