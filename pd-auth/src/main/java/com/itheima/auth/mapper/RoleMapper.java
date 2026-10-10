package com.itheima.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itheima.auth.entity.Role;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色 Mapper。走租户插件自动隔离。
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {
}
