package com.itheima.pinda.zuul.biz.service.impl;

import com.itheima.pinda.authority.api.RoleApi;
import com.itheima.pinda.authority.dto.auth.RoleResourceDTO;
import com.itheima.pinda.base.R;
import com.itheima.pinda.common.constant.CacheKey;
import com.itheima.pinda.zuul.biz.service.RoleAuthService;
import lombok.extern.slf4j.Slf4j;
import net.oschina.j2cache.Cache;
import net.oschina.j2cache.CacheChannel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class RoleAuthServiceImpl implements RoleAuthService {
    @Autowired
    private CacheChannel cache;

    @Autowired
    RoleApi roleApi;

    @Override
    public List<RoleResourceDTO> findAllRoles() {
        Cache provider = cache.getL2Provider().buildCache(CacheKey.ROLE_RESOURCE, null);
        Object value = provider.get(CacheKey.ALL_ROLES_WITH_RESOURCE);

        List<RoleResourceDTO> list;
        if (value == null) {
            R<List<RoleResourceDTO>> allRoles = roleApi.findAllRoles();
            log.info("AUTH-findAllRoles 缓存为空 重新获取");
            list = allRoles.getData();
            if (list == null) {
                list = Collections.EMPTY_LIST;
            }
            provider.put(CacheKey.ALL_ROLES_WITH_RESOURCE, list);
        } else {
            list = (List<RoleResourceDTO>) value;
        }
        return list;
    }

    @Override
    public List<Long> findRoleByUserId(Long id) {
        String key = CacheKey.buildKey(id);

        Cache provider = cache.getL2Provider().buildCache(CacheKey.USER_ROLE, null);
        Object value = provider.get(key);
        List<Long> list;
        if (value == null) {
            R<List<Long>> roles = roleApi.findRoleByUserId(id);
            log.info("AUTH-findAllRoles 缓存为空 重新获取");
            list = roles.getData();
            if (list == null) {
                list = Collections.EMPTY_LIST;
            }
            provider.put(key, list);
        } else {
            list = (List<Long>) value;
        }
        return list;
    }
}
