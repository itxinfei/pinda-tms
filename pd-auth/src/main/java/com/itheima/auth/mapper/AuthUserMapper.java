package com.itheima.auth.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itheima.auth.entity.AuthUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper。登录发生在认证之前，关闭租户插件、由查询条件显式携带 tenant_id。
 */
@Mapper
@InterceptorIgnore(tenantLine = "1")
public interface AuthUserMapper extends BaseMapper<AuthUser> {
}
