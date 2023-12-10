package com.itheima.pinda.authority.api.hystrix;

import java.util.Map;

import com.itheima.pinda.authority.api.AuthorityGeneralApi;
import com.itheima.pinda.base.R;

import org.springframework.stereotype.Component;

/**
 * 查询通用数据
 */
@Component
public class AuthorityGeneralApiFallback implements AuthorityGeneralApi {
    @Override
    public R<Map<String, Map<String, String>>> enums() {
        return R.timeout();
    }
}
