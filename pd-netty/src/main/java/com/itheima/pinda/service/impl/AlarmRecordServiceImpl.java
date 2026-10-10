package com.itheima.pinda.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.itheima.pinda.entity.AlarmRecord;
import com.itheima.pinda.mapper.AlarmRecordMapper;
import com.itheima.pinda.service.IAlarmRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * GPS告警记录 Service 实现
 */
@Slf4j
@Service
public class AlarmRecordServiceImpl extends ServiceImpl<AlarmRecordMapper, AlarmRecord> implements IAlarmRecordService {

    /**
     * 未处理状态
     */
    private static final int STATUS_UNHANDLED = 0;

    /**
     * 无业务主体告警的时间窗去重（分钟）——如 VEHICLE_OFFLINE 批量离线告警，
     * 扫描周期 60s 会反复命中，靠该窗口防止刷屏；外部化配置，禁硬编码
     */
    @Value("${gps.alert.global-dedupe-window-minutes:10}")
    private long globalDedupeWindowMinutes;

    /**
     * 幂等写入告警记录（2026-10-10 P0-5 扩展去重维度）
     *
     * <p>去重优先级：transport_task_id → truck_id → driver_id → 无主体时间窗兜底，
     * 均限定"同告警类型 + 未处理"。</p>
     *
     * @param alarmRecord 告警记录
     * @return true-本次新插入 false-命中未处理记录被跳过
     */
    @Override
    public boolean saveIfNotDuplicate(AlarmRecord alarmRecord) {
        LambdaQueryWrapper<AlarmRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlarmRecord::getAlarmType, alarmRecord.getAlarmType());
        wrapper.eq(AlarmRecord::getStatus, STATUS_UNHANDLED);
        if (StringUtils.hasText(alarmRecord.getTransportTaskId())) {
            wrapper.eq(AlarmRecord::getTransportTaskId, alarmRecord.getTransportTaskId());
        } else if (StringUtils.hasText(alarmRecord.getTruckId())) {
            wrapper.eq(AlarmRecord::getTruckId, alarmRecord.getTruckId());
        } else if (StringUtils.hasText(alarmRecord.getDriverId())) {
            wrapper.eq(AlarmRecord::getDriverId, alarmRecord.getDriverId());
        } else {
            // 无业务主体（如批量离线告警，仅有条数无车辆明细）：按类型+未处理+时间窗去重
            wrapper.ge(AlarmRecord::getAlarmTime, LocalDateTime.now().minusMinutes(globalDedupeWindowMinutes));
        }
        // MP 3.5.x selectCount 返回 Long
        Integer count = Math.toIntExact(baseMapper.selectCount(wrapper));
        if (count != null && count > 0) {
            log.info("[GPS告警] 已存在相同维度的未处理告警，跳过落库: taskId={}, truckId={}, type={}",
                    alarmRecord.getTransportTaskId(), alarmRecord.getTruckId(), alarmRecord.getAlarmType());
            return false;
        }
        return save(alarmRecord);
    }
}
