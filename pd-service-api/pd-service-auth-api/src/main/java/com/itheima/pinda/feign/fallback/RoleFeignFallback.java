package com.itheima.pinda.feign.fallback;

import com.itheima.pinda.DTO.RoleDTO;
import com.itheima.pinda.feign.RoleFeign;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 角色 Feign 熔断降级
 */
@Slf4j
@Component
public class RoleFeignFallback implements RoleFeign {

    @Override
    public List<RoleDTO> list(Long userId) {
        log.warn("远程调用 pd-auth 失败: RoleFeign.list({}), 返回空列表", userId);
        return Collections.emptyList();
    }
}
