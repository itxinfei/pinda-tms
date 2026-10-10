package com.itheima.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itheima.auth.entity.CoreStation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 岗位 Mapper。走租户插件自动隔离。
 */
@Mapper
public interface CoreStationMapper extends BaseMapper<CoreStation> {
}
