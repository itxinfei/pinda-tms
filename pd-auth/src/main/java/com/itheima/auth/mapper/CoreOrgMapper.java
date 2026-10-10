package com.itheima.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itheima.auth.entity.CoreOrg;
import org.apache.ibatis.annotations.Mapper;

/**
 * 组织 Mapper。走租户插件自动隔离。
 */
@Mapper
public interface CoreOrgMapper extends BaseMapper<CoreOrg> {
}
