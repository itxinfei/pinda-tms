package com.itheima.pinda.authority.api;

import java.util.Map;

import com.itheima.pinda.authority.api.hystrix.AuthorityGeneralApiFallback;
import com.itheima.pinda.base.R;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 通用API
 */
@FeignClient(name = "${pinda.feign.authority-server:pd-auth-server}", fallback = AuthorityGeneralApiFallback.class)
public interface AuthorityGeneralApi {
    /**
     * 查询系统中常用的枚举类型等
     *
     * @return
     */
    @GetMapping("/enums")
    R<Map<String, Map<String, String>>> enums();
}
