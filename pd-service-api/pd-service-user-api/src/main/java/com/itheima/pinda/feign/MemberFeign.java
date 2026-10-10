package com.itheima.pinda.feign;

import com.itheima.pinda.common.utils.PageResponse;
import com.itheima.pinda.common.utils.Result;
import com.itheima.pinda.entity.Member;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Hidden;

// 不要在接口上加类级 @RequestMapping；路径前缀下沉到方法，避免被 MVC 注册与本地 Controller 冲突。
@FeignClient(name = "pd-user")
@Hidden
public interface MemberFeign {
    /**
     * 分页查询
     *
     * @param page
     * @param pageSize
     * @return
     */
    @GetMapping("/member/page")
    PageResponse<Member> page(@RequestParam("page") Integer page,@RequestParam("pageSize") Integer pageSize);

    /**
     * 新增
     *
     * @param entity
     * @return
     */
    @PostMapping("/member")
    Result save(@RequestBody Member entity);

    /**
     * 修改
     *
     * @param id
     * @param entity
     * @return
     */
    @PutMapping("/member/{id}")
    Result update(@PathVariable(name = "id") String id, @RequestBody Member entity);

    /**
     * 删除
     *
     * @param id
     * @return
     */
    @DeleteMapping("/member/{id}")
    Result del(@PathVariable(name = "id") String id);

    /**
     * 详情
     *
     * @param id
     * @return
     */
    @GetMapping("/member/detail/{id}")
    Member detail(@PathVariable(name = "id") String id);
}
