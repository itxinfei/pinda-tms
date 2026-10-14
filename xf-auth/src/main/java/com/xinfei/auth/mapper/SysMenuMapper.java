package com.xinfei.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinfei.auth.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;

/**
 * 菜单/按钮 Mapper（全局平台表，租户插件已忽略 sys_menu）。
 */
@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {
}
