package com.itheima.pinda.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itheima.pinda.entity.AlarmRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * GPS告警记录 Mapper
 */
@Mapper
public interface AlarmRecordMapper extends BaseMapper<AlarmRecord> {
}
