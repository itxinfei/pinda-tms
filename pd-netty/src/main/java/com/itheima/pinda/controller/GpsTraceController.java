package com.itheima.pinda.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.pinda.common.utils.Result;
import com.itheima.pinda.entity.LocationRecord;
import com.itheima.pinda.service.ILocationRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
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
 * <p>基于落库的 {@link LocationRecord}（pd_truck_location 表）提供：
 * 轨迹回放、最近位置、分页查询能力，供管理端车辆轨迹监控页面使用。</p>
 */
@Slf4j
@RestController
@RequestMapping("/trace")
@Api(tags = "车辆轨迹查询")
public class GpsTraceController {

    @Autowired
    private ILocationRecordService locationRecordService;

    /**
     * 轨迹回放：按业务ID（车辆/快递员）+ 类型查询完整轨迹（按上报时间升序）
     *
     * @param businessId 业务ID（车辆ID或快递员ID）
     * @param type       类型：truck-车辆 courier-快递员（可选）
     * @return 轨迹点列表
     */
    @ApiOperation(value = "轨迹回放")
    @GetMapping("/replay")
    public Result replay(@RequestParam("businessId") String businessId,
                         @RequestParam(value = "type", required = false) String type) {
        if (StringUtils.isBlank(businessId)) {
            return Result.error("businessId不能为空");
        }
        LambdaQueryWrapper<LocationRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LocationRecord::getBusinessId, businessId);
        if (StringUtils.isNotBlank(type)) {
            wrapper.eq(LocationRecord::getType, type);
        }
        // 按上报时间升序，还原行驶轨迹
        wrapper.orderByAsc(LocationRecord::getCurrentTime);
        List<LocationRecord> records = locationRecordService.list(wrapper);
        return Result.ok().put("data", records);
    }

    /**
     * 最近位置：按业务ID + 类型查询最新一条位置
     *
     * @param businessId 业务ID
     * @param type       类型（可选）
     * @return 最新位置记录
     */
    @ApiOperation(value = "最近位置")
    @GetMapping("/latest")
    public Result latest(@RequestParam("businessId") String businessId,
                         @RequestParam(value = "type", required = false) String type) {
        if (StringUtils.isBlank(businessId)) {
            return Result.error("businessId不能为空");
        }
        LambdaQueryWrapper<LocationRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LocationRecord::getBusinessId, businessId);
        if (StringUtils.isNotBlank(type)) {
            wrapper.eq(LocationRecord::getType, type);
        }
        wrapper.orderByDesc(LocationRecord::getCurrentTime);
        wrapper.last("limit 1");
        LocationRecord record = locationRecordService.getOne(wrapper);
        return Result.ok().put("data", record);
    }

    /**
     * 分页查询：按业务ID/类型/运输任务/车牌号筛选
     *
     * @param params page、pageSize、businessId、type、transportTaskId、licensePlate
     * @return 分页结果
     */
    @ApiOperation(value = "轨迹分页查询")
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
        if (params.get("businessId") != null && StringUtils.isNotBlank(params.get("businessId").toString())) {
            wrapper.like(LocationRecord::getBusinessId, params.get("businessId").toString());
        }
        if (params.get("type") != null && StringUtils.isNotBlank(params.get("type").toString())) {
            wrapper.eq(LocationRecord::getType, params.get("type").toString());
        }
        if (params.get("transportTaskId") != null && StringUtils.isNotBlank(params.get("transportTaskId").toString())) {
            wrapper.eq(LocationRecord::getTransportTaskId, params.get("transportTaskId").toString());
        }
        if (params.get("licensePlate") != null && StringUtils.isNotBlank(params.get("licensePlate").toString())) {
            wrapper.like(LocationRecord::getLicensePlate, params.get("licensePlate").toString());
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
}
