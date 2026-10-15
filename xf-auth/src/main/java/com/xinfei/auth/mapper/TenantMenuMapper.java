package com.xinfei.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinfei.auth.entity.SysTenantMenu;
import org.apache.ibatis.annotations.Mapper;

/**
 * 租户-菜单关系 Mapper。
 */
@Mapper
public interface TenantMenuMapper extends BaseMapper<SysTenantMenu> {
}
