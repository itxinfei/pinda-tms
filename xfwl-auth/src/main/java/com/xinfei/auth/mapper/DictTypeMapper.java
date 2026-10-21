package com.xinfei.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinfei.auth.entity.DictType;
import org.apache.ibatis.annotations.Mapper;

/**
 * 字典类型 Mapper（走租户插件自动隔离）。
 */
@Mapper
public interface DictTypeMapper extends BaseMapper<DictType> {
}
