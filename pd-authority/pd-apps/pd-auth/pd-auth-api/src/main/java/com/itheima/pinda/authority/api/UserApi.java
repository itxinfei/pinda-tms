package com.itheima.pinda.authority.api;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.pinda.authority.api.hystrix.UserApiFallback;
import com.itheima.pinda.authority.entity.auth.User;
import com.itheima.pinda.base.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户
 *
 * @author User
 */
@FeignClient(name = "${pinda.feign.authority-server:pd-auth-server}", fallback = UserApiFallback.class, path = "/user")
public interface UserApi {
    /**
     * 刷新token
     *
     * @param id 用户id
     * @return
     */
    @RequestMapping(value = "/ds/{id}", method = RequestMethod.GET)
    Map<String, Object> getDataScopeById(@PathVariable("id") Long id);

    /**
     * 查询所有的用户id
     *
     * @return
     */
    @RequestMapping(value = "/find", method = RequestMethod.GET)
    R<List<Long>> findAllUserId();

    /**
     * 查询用户
     *
     * @param id 主键id
     * @return 查询结果
     */
    @GetMapping("/{id}")
    R<User> get(@PathVariable Long id);

    /**
     * 分页查询用户
     *
     * @param current   页码
     * @param size      页尺寸
     * @param orgId     组织id
     * @param stationId 岗位id
     * @param name      用户名称
     * @param account   用户账号
     * @param mobile    手机号码
     * @return 查询结果
     */
    @GetMapping("/page")
    R<Page<User>> page(@RequestParam("current") Long current, @RequestParam("size") Long size, @RequestParam(name = "orgId", required = false) Long orgId, @RequestParam(name = "stationId", required = false) Long stationId, @RequestParam(name = "name", required = false) String name, @RequestParam(name = "account", required = false) String account, @RequestParam(name = "mobile", required = false) String mobile);

    /**
     * 根据条件获取用户列表
     *
     * @param ids       用户id列表
     * @param stationId 岗位id
     * @param name      用户名称
     * @param orgId     组织id
     * @return 用户列表
     */
    @GetMapping("")
    R<List<User>> list(@RequestParam(name = "ids", required = false) List<Long> ids, @RequestParam(name = "stationId", required = false) Long stationId, @RequestParam(name = "name", required = false) String name, @RequestParam(name = "orgId", required = false) Long orgId);
}
