package com.itheima.pinda.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.pinda.common.utils.Result;
import com.itheima.pinda.DTO.ExceptionReportDTO;
import com.itheima.pinda.entity.AlarmRecord;
import com.itheima.pinda.enums.ExceptionType;
import com.itheima.pinda.service.IAlarmRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * GPS告警查询与处置接口
 *
 * <p>基于落库的 {@link AlarmRecord}（pd_alarm_record 表）提供：
 * 按运输任务/告警类型过滤的分页查询、标记已处理能力，供管理端告警监控页面使用。</p>
 */
@Slf4j
@RestController
@RequestMapping("/alarm")
@Tag(name = "GPS告警查询与处置")
public class AlarmController {

    @Autowired
    private IAlarmRecordService alarmRecordService;

    /**
     * 分页查询：按运输任务ID/告警类型/处理状态筛选
     *
     * @param transportTaskId 运输任务ID（可选）
     * @param alarmType       告警类型（可选）：SPEED_OVER/STAY_TOO_LONG/DEVIATE_ROUTE
     * @param status          处理状态（可选）：0-未处理 1-已处理
     * @param page            页码，默认1
     * @param pageSize        每页条数，默认10，最大200
     * @return 分页结果
     */
    @Operation(summary = "告警分页查询")
    @GetMapping
    public Result page(@RequestParam(value = "transportTaskId", required = false) String transportTaskId,
                       @RequestParam(value = "alarmType", required = false) String alarmType,
                       @RequestParam(value = "status", required = false) Integer status,
                       @RequestParam(value = "page", defaultValue = "1") Integer page,
                       @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        // 分页参数防御性校验：非法或越界时返回 400，避免 500
        if (page == null || pageSize == null || page < 1 || pageSize < 1 || pageSize > 200) {
            log.warn("[告警查询] 分页参数越界: page={}, pageSize={}", page, pageSize);
            return Result.error(400, "分页参数越界：page>=1，1<=pageSize<=200");
        }

        LambdaQueryWrapper<AlarmRecord> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(transportTaskId)) {
            wrapper.eq(AlarmRecord::getTransportTaskId, transportTaskId);
        }
        if (StringUtils.isNotBlank(alarmType)) {
            wrapper.eq(AlarmRecord::getAlarmType, alarmType);
        }
        if (status != null) {
            wrapper.eq(AlarmRecord::getStatus, status);
        }
        wrapper.orderByDesc(AlarmRecord::getAlarmTime);

        IPage<AlarmRecord> result = alarmRecordService.page(new Page<>(page, pageSize), wrapper);
        Map<String, Object> data = new HashMap<>();
        data.put("items", result.getRecords());
        data.put("total", result.getTotal());
        data.put("pages", result.getPages());
        data.put("page", page);
        data.put("pageSize", pageSize);
        return Result.ok().put("data", data);
    }

    /**
     * 标记告警已处理
     *
     * @param id 告警记录ID
     * @return 更新后的告警记录
     */
    @Operation(summary = "标记告警已处理")
    @PutMapping("/{id}/handle")
    public Result handle(@PathVariable("id") String id) {
        if (StringUtils.isBlank(id)) {
            return Result.error(400, "id不能为空");
        }
        AlarmRecord record = alarmRecordService.getById(id);
        if (record == null) {
            return Result.error(404, "告警记录不存在");
        }
        // 已是已处理状态则幂等返回成功，不重复更新
        if (record.getStatus() != null && record.getStatus() == 1) {
            return Result.ok().put("data", record);
        }
        record.setStatus(1);
        boolean updated = alarmRecordService.updateById(record);
        if (!updated) {
            return Result.error("标记已处理失败");
        }
        return Result.ok().put("data", record);
    }

    /**
     * 一线人员异常上报（移动三端 · D-41 快递员异常上报 / D-57 司机在途异常上报）
     *
     * <p>由快递员端与司机端在 App 上<b>主动上报</b>异常，落库复用 {@link AlarmRecord}
     * （pd_alarm_record）——与 GPS 自动检测告警（SPEED_OVER 等）同表共存，按
     * {@code alarmType} 过滤即可区分来源。</p>
     *
     * <p><b>入参遵循定案 D-46「类型 + 照片 + 备注」三要素</b>：三者缺一不可，
     * 其中 {@code attachmentIds} 为必填——照片是合规凭证，无凭证的异常不予受理。</p>
     *
     * @param dto 上报入参
     * @return 落库后的告警记录（含id 供前端追单）
     */
    @Operation(summary = "异常上报（快递员/司机）")
    @PostMapping("/report")
    public Result report(@RequestBody ExceptionReportDTO dto) {
        // 1. 入参基础校验（三要素：D-46）
        if (dto == null) {
            return Result.error(400, "上报内容不能为空");
        }
        if (StringUtils.isBlank(dto.getExceptionType())) {
            return Result.error(400, "异常类型不能为空");
        }
        ExceptionType exceptionType = ExceptionType.of(dto.getExceptionType());
        if (exceptionType == null) {
            return Result.error(400, "异常类型不合法，可选值：" + Arrays.toString(ExceptionType.values()));
        }
        if (StringUtils.isBlank(dto.getReporterId())) {
            return Result.error(400, "上报人ID不能为空");
        }
        if (dto.getAttachmentIds() == null || dto.getAttachmentIds().isEmpty()) {
            // 照片是合规凭证，无凭证不予受理（D-46）
            return Result.error(400, "必须上传照片凭证（attachmentIds 不能为空）");
        }
        if (StringUtils.isBlank(dto.getRemark())) {
            return Result.error(400, "备注不能为空");
        }
        // 角色与类型一致性校验：快递员侧只能报配送类，误报会被拦下
        if (dto.isCourier() && !ExceptionType.isCourierScoped(dto.getExceptionType())) {
            return Result.error(400, "该异常类型不属于配送环节可上报范围");
        }

        // 2. 组装并落库（复用既有幂等写入）
        LocalDateTime now = LocalDateTime.now();
        AlarmRecord record = new AlarmRecord();
        record.setAlarmType(exceptionType.name());
        record.setTransportTaskId(dto.getTransportTaskId());
        // 上报人按角色分别记入 driverId（快递员/司机均属"人"，与 GPS 侧车辆区分）
        record.setDriverId(dto.getReporterId());
        record.setLongitude(dto.getLongitude());
        record.setLatitude(dto.getLatitude());
        record.setAlarmContent(truncate(dto.getRemark() + " | 凭证：" + dto.getAttachmentIds(), 500));
        record.setAlarmTime(now);
        record.setStatus(0);
        record.setCreateTime(now);
        boolean inserted = alarmRecordService.saveIfNotDuplicate(record);
        if (!inserted) {
            // 命中幂等：同一任务 + 同类型已有未处理记录，视为重复上报
            log.info("[异常上报] 命中幂等跳过: reporter={}, type={}", dto.getReporterId(), exceptionType);
            return Result.ok().put("data", record).put("duplicated", true);
        }
        log.info("[异常上报] 已受理: reporter={}, type={}, task={}", dto.getReporterId(), exceptionType, dto.getTransportTaskId());
        return Result.ok().put("data", record);
    }

    /**
     * 按最大长度截断字符串，避免超过列长度导致整条记录落库失败
     *
     * @param value     原始字符串
     * @param maxLength 最大长度
     * @return 截断后的字符串
     */
    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
