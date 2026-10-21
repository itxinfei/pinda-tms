package com.xinfei.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinfei.auth.entity.DictItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 字典项 Mapper（走租户插件自动隔离）。
 */
@Mapper
public interface DictItemMapper extends BaseMapper<DictItem> {
}
