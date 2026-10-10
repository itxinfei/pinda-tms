package com.itheima.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itheima.auth.entity.AuthUserRole;
import org.apache.ibatis.annotations.Mapper;

/**
 * 账号角色绑定 Mapper。走租户插件自动隔离。
 */
@Mapper
public interface AuthUserRoleMapper extends BaseMapper<AuthUserRole> {
}
