package com.itheima.pinda.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itheima.pinda.entity.ScheduleExceptionOrder;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Component;

/**
 * 异常调度订单 Mapper
 */
@Component
@Mapper
public interface ScheduleExceptionOrderMapper extends BaseMapper<ScheduleExceptionOrder> {
}
