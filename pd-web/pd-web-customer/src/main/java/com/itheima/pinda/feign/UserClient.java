package com.itheima.pinda.feign;

import com.itheima.pinda.DTO.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 用户账号查询客户端（指向新 pd-auth）。
 *
 * <p>旧实现通过 {@code url=${client.user.url}} 直连已废弃的 pd-authority 的
 * {@code auth/query/id}（返回 Map 包装）。现改为服务发现方式走 pd-auth 内部端点，
 * 直接返回 {@link UserDTO}，不再依赖硬编码 URL 与 data 包装。</p>
 */
@FeignClient(name = "pd-auth")
public interface UserClient {

    @GetMapping("/internal/user/{id}")
    UserDTO findById(@PathVariable("id") Long id);

}
