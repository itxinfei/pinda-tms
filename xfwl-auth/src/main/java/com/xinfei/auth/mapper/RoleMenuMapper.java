package com.xinfei.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinfei.auth.entity.RoleMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色-菜单授权 Mapper（走租户插件自动隔离）。
 */
@Mapper
public interface RoleMenuMapper extends BaseMapper<RoleMenu> {

    /** 按角色 id 集合查授权的菜单/按钮 id（去重） */
    @Select("<script>SELECT DISTINCT menu_id FROM sys_role_menu WHERE role_id IN " +
            "<foreach collection='roleIds' item='r' open='(' separator=',' close=')'>#{r}</foreach></script>")
    List<Long> selectMenuIdsByRoleIds(@Param("roleIds") List<Long> roleIds);
}
