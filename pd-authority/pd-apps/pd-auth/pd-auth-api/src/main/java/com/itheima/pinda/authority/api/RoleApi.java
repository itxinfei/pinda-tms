package com.itheima.pinda.authority.api;

import java.util.List;

import com.itheima.pinda.authority.api.hystrix.RoleApiFallback;
import com.itheima.pinda.authority.dto.auth.RoleDTO;
import com.itheima.pinda.authority.dto.auth.RoleResourceDTO;
import com.itheima.pinda.base.R;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * 角色API
 */
@FeignClient(name = "${pinda.feign.authority-server:pd-auth-server}", path = "/role", fallback = RoleApiFallback.class)
public interface RoleApi {
    /**
     * 根据角色编码，查找用户id
     *
     * @param codes 角色编码
     * @return
     */
    @GetMapping("/codes")
    R<List<Long>> findUserIdByCode(@RequestParam(value = "codes") String[] codes);

    /**
     * 查询角色
     *
     * @return
     */
    @RequestMapping(value = "/findRoleByUserId/{id}", method = RequestMethod.GET)
    R<List<Long>> findRoleByUserId(@PathVariable("id") Long id);

    /**
     * 查询全部角色和资源
     *
     * @return
     */
    @RequestMapping(value = "/findAllRoles", method = RequestMethod.GET)
    R<List<RoleResourceDTO>> findAllRoles();

    /**
     * 根据条件查询角色列表
     *
     * @param userId 用户id
     * @return 角色列表
     */
    @GetMapping
    R<List<RoleDTO>> list(@RequestParam("userId") Long userId);
}
