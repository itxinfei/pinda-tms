package com.xinfei.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinfei.auth.entity.SysTenantPlanMenu;
import org.apache.ibatis.annotations.Mapper;

/**
 * 租户套餐-菜单关系 Mapper。
 */
@Mapper
public interface SysTenantPlanMenuMapper extends BaseMapper<SysTenantPlanMenu> {
}