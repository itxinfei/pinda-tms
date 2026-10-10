package com.itheima.pinda.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.itheima.pinda.entity.AlarmRecord;

/**
 * GPS告警记录 Service
 */
public interface IAlarmRecordService extends IService<AlarmRecord> {

    /**
     * 幂等写入告警记录
     *
     * <p>transportTaskId 非空时，若已存在同一 transport_task_id + alarm_type 且
     * status=0（未处理）的记录则不重复插入；拿不到 transportTaskId 时直接插入。</p>
     *
     * @param alarmRecord 告警记录
     * @return true-本次新插入 false-命中未处理记录被跳过
     */
    boolean saveIfNotDuplicate(AlarmRecord alarmRecord);
}
