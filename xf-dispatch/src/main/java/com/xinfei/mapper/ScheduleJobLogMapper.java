package com.xinfei.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinfei.entity.ScheduleJobLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Component;

@Component
@Mapper
public interface ScheduleJobLogMapper extends BaseMapper<ScheduleJobLogEntity> {

}
