package com.itheima.pinda.authority.api.hystrix;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.pinda.authority.api.UserApi;
import com.itheima.pinda.authority.dto.auth.UserDTO;
import com.itheima.pinda.authority.entity.auth.User;
import com.itheima.pinda.base.R;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户API熔断
 */
@Component
public class UserApiFallback implements UserApi {

    @Override
    public Map<String, Object> getDataScopeById(Long id) {
        Map<String, Object> map = new HashMap<>(2);
        map.put("dsType", 5);
        map.put("orgIds", Collections.emptyList());
        return map;
    }

    @Override
    public R<List<Long>> findAllUserId() {
        return R.timeout();
    }

    @Override
    public R<User> get(Long id) {
        return R.timeout();
    }

    @Override
    public R<Page<User>> page(Long current, Long size, Long orgId, Long stationId, String name, String account, String mobile) {
        return R.timeout();
    }

    @Override
    public R<List<User>> list(List<Long> ids, Long stationId, String name, Long orgId) {
        return R.timeout();
    }
}
