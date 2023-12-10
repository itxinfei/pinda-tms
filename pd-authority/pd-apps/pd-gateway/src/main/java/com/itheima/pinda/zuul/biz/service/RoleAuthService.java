package com.itheima.pinda.zuul.biz.service;

import com.itheima.pinda.authority.dto.auth.RoleResourceDTO;

import java.util.List;

public interface RoleAuthService {

    List<RoleResourceDTO> findAllRoles();

    List<Long> findRoleByUserId(Long id);
}
