package com.itheima.pinda.feign;

import com.itheima.pinda.DTO.MemberAutoCreateDTO;
import com.itheima.pinda.DTO.MemberDTO;
import com.itheima.pinda.common.utils.PageResponse;
import com.itheima.pinda.common.utils.Result;
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
    PageResponse<MemberDTO> page(@RequestParam("page") Integer page,@RequestParam("pageSize") Integer pageSize);

    /**
     * 新增
     *
     * @param entity
     * @return
     */
    @PostMapping("/member")
    Result save(@RequestBody MemberDTO entity);

    /**
     * 修改
     *
     * @param id
     * @param entity
     * @return
     */
    @PutMapping("/member/{id}")
    Result update(@PathVariable(name = "id") String id, @RequestBody MemberDTO entity);

    /**
     * 删除
     *
     * @param id
     * @return
     */
    @DeleteMapping("/member/{id}")
    Result del(@PathVariable(name = "id") String id);

    /**
     * 详情（鉴权失败/不存在返回 null）
     *
     * @param id
     * @return
     */
    @GetMapping("/member/detail/{id}")
    MemberDTO detail(@PathVariable(name = "id") String id);

    /**
     * 内部端点：按租户+手机号查会员（不存在返回 null）
     *
     * @param phone 手机号
     * @return 会员；不存在返回 null
     */
    @GetMapping("/member/internal/phone")
    MemberDTO getByPhone(@RequestParam("phone") String phone);

    /**
     * 内部端点：按租户+手机号查会员，不存在则自动建档；并发建档由 uk_tenant_phone 唯一键兜底
     *
     * @param dto 手机号/昵称/头像（租户走 tenantId 请求头，不进 DTO）
     * @return 会员；入参手机号为空返回 null
     */
    @PostMapping("/member/internal/get-or-create")
    MemberDTO getOrCreate(@RequestBody MemberAutoCreateDTO dto);
}
