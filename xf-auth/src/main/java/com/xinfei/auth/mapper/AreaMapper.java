package com.xinfei.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinfei.auth.entity.Area;
import org.apache.ibatis.annotations.Mapper;

/**
 * 行政区划 Mapper。全局共享表，已在租户插件 ignoreTable 中放行。
 */
@Mapper
public interface AreaMapper extends BaseMapper<Area> {
}
