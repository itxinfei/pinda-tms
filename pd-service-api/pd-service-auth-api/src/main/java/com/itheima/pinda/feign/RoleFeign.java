package com.itheima.pinda.feign;

import com.itheima.pinda.DTO.RoleDTO;
import com.itheima.pinda.feign.fallback.RoleFeignFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 角色内部 Feign。
 */
@FeignClient(value = "pd-auth", fallback = RoleFeignFallback.class)
public interface RoleFeign {

    /**
     * 查询指定用户拥有的角色（id、name）
     */
    @GetMapping("/internal/role/list")
    List<RoleDTO> list(@RequestParam("userId") Long userId);
}
