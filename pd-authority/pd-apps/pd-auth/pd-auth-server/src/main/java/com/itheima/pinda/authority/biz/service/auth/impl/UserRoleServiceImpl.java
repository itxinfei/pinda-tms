package com.itheima.pinda.authority.biz.service.auth.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.pinda.authority.biz.dao.auth.UserRoleMapper;
import com.itheima.pinda.authority.entity.auth.UserRole;
import com.itheima.pinda.authority.biz.service.auth.UserRoleService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.itheima.pinda.common.constant.BizConstant.INIT_ROLE_ID;

/**
 * <p>
 * 业务实现类
 * 角色分配
 * 账号角色绑定
 * </p>
 *
 */
@Slf4j
@Service
public class UserRoleServiceImpl extends ServiceImpl<UserRoleMapper, UserRole> implements UserRoleService {

    @Override
    public void initAdmin(Long userId) {
        UserRole userRole = UserRole.builder()
                .userId(userId).roleId(INIT_ROLE_ID)
                .build();

        super.save(userRole);
    }
}
