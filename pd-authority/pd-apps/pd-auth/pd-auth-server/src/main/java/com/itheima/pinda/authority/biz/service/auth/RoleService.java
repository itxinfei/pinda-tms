package com.itheima.pinda.authority.biz.service.auth;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.pinda.authority.dto.auth.*;
import com.itheima.pinda.authority.entity.auth.Role;
import com.itheima.pinda.authority.dto.auth.RoleSaveDTO;
import com.itheima.pinda.authority.dto.auth.RoleUpdateDTO;

/**
 * <p>
 * 业务接口
 * 角色
 * </p>
 *
 */
public interface RoleService extends IService<Role> {

    /**
     * 根据ID查
     *
     * @param id 主键
     * @return
     */
    Role getByIdWithCache(Long id);

    /**
     * 根据ID删除
     *
     * @param ids
     * @return
     */
    boolean removeByIdWithCache(List<Long> ids);

    /**
     * 判断用户是否超级管理员
     * @param userId
     * @return
     */
    boolean isSuperAdmin(Long userId);

    /**
     * 查询用户拥有的角色
     *
     * @param userId
     * @return
     */
    List<Role> findRoleByUserId(Long userId);

    /**
     * 保存角色
     *
     * @param data
     * @param userId 用户id
     */
    void saveRole(RoleSaveDTO data, Long userId);

    /**
     * 修改
     *
     * @param role
     * @param userId
     */
    void updateRole(RoleUpdateDTO role, Long userId);

    /**
     * 根据角色编码查询用户ID
     *
     * @param codes 角色编码
     * @return
     */
    List<Long> findUserIdByCode(String[] codes);

    /**
     * 检测编码重复
     *
     * @param code
     * @return 存在返回真
     */
    Boolean check(String code);

    List<RoleResourceDTO> findAllRolesWithResource();
}
