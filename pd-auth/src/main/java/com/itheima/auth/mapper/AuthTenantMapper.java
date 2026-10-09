package com.itheima.auth.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itheima.auth.entity.AuthTenant;
import org.apache.ibatis.annotations.Mapper;

/**
 * 租户 Mapper。平台表 + 登录前访问，关闭租户插件。
 */
@Mapper
@InterceptorIgnore(tenantLine = "1")
public interface AuthTenantMapper extends BaseMapper<AuthTenant> {
}
