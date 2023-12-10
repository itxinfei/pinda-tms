package com.itheima.pinda.authority.biz.service.auth;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.pinda.authority.entity.auth.UserRole;
import com.itheima.pinda.authority.entity.auth.UserRole;

/**
 * <p>
 * 业务接口
 * 角色分配
 * 账号角色绑定
 * </p>
 *
 */
public interface UserRoleService extends IService<UserRole> {
    /**
     * 初始化超级管理员角色 权限
     *
     * @param userId
     */
    void initAdmin(Long userId);
}
