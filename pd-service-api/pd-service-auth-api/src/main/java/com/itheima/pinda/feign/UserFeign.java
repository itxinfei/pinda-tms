package com.itheima.pinda.feign;

import com.itheima.pinda.DTO.UserDTO;
import com.itheima.pinda.common.utils.PageResponse;
import com.itheima.pinda.feign.fallback.UserFeignFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 用户（账号）内部 Feign。
 */
@FeignClient(value = "pd-auth", fallback = UserFeignFallback.class)
public interface UserFeign {

    /**
     * 按ID查询用户
     */
    @GetMapping("/internal/user/{id}")
    UserDTO get(@PathVariable("id") Long id);

    /**
     * 多条件查询用户列表
     *
     * @param ids       用户ID集合
     * @param stationId 岗位ID
     * @param name      姓名
     * @param orgId     组织ID
     */
    @GetMapping("/internal/user/list")
    List<UserDTO> list(@RequestParam(value = "ids", required = false) List<Long> ids,
                       @RequestParam(value = "stationId", required = false) Long stationId,
                       @RequestParam(value = "name", required = false) String name,
                       @RequestParam(value = "orgId", required = false) Long orgId);

    /**
     * 分页查询用户
     */
    @GetMapping("/internal/user/page")
    PageResponse<UserDTO> page(@RequestParam("page") long page,
                               @RequestParam("size") long size,
                               @RequestParam(value = "orgId", required = false) Long orgId,
                               @RequestParam(value = "stationId", required = false) Long stationId,
                               @RequestParam(value = "name", required = false) String name,
                               @RequestParam(value = "account", required = false) String account,
                               @RequestParam(value = "mobile", required = false) String mobile);
}
