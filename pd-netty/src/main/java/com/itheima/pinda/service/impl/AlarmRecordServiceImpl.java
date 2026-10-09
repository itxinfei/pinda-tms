package com.itheima.pinda.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.pinda.entity.AlarmRecord;
import com.itheima.pinda.mapper.AlarmRecordMapper;
import com.itheima.pinda.service.IAlarmRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
     * 幂等写入告警记录
     *
     * @param alarmRecord 告警记录
     * @return true-本次新插入 false-命中未处理记录被跳过
     */
    @Override
    public boolean saveIfNotDuplicate(AlarmRecord alarmRecord) {
        // 拿不到 taskId 时不做去重，直接插入
        if (StringUtils.hasText(alarmRecord.getTransportTaskId())) {
            LambdaQueryWrapper<AlarmRecord> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(AlarmRecord::getTransportTaskId, alarmRecord.getTransportTaskId());
            wrapper.eq(AlarmRecord::getAlarmType, alarmRecord.getAlarmType());
            wrapper.eq(AlarmRecord::getStatus, STATUS_UNHANDLED);
            // MyBatis-Plus 3.3.0 selectCount 返回 Integer（3.4+ 才是 Long）
            Integer count = baseMapper.selectCount(wrapper);
            if (count != null && count > 0) {
                log.info("[GPS告警] 已存在相同任务的未处理告警，跳过落库: taskId={}, type={}",
                        alarmRecord.getTransportTaskId(), alarmRecord.getAlarmType());
                return false;
            }
        }
        return save(alarmRecord);
    }
}
