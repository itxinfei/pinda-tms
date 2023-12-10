package com.itheima.pinda.authority.api.hystrix;

import com.itheima.pinda.authority.api.RoleApi;
import com.itheima.pinda.authority.dto.auth.RoleDTO;
import com.itheima.pinda.authority.dto.auth.RoleResourceDTO;
import com.itheima.pinda.base.R;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 角色查询API
 */
@Component
public class RoleApiFallback implements RoleApi {
    @Override
    public R<List<Long>> findUserIdByCode(String[] codes) {
        return R.timeout();
    }

    @Override
    public R<List<Long>> findRoleByUserId(Long id) {
        return R.timeout();
    }

    @Override
    public R<List<RoleResourceDTO>> findAllRoles() {
        return R.timeout();
    }

    @Override
    public R<List<RoleDTO>> list(Long userId) {
        return R.timeout();
    }
}
