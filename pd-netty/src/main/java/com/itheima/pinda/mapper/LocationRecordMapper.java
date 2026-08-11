package com.itheima.pinda.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itheima.pinda.entity.LocationRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * GPS轨迹明细 Mapper
 */
@Mapper
public interface LocationRecordMapper extends BaseMapper<LocationRecord> {
}
