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
     * @param page     页码
     * @param pageSize 每页条数
     * @param phone    手机号（模糊匹配，可空）
     * @param status   状态：1 正常 0 禁用，不传查全部
     * @return
     */
    @GetMapping("/member/page")
    PageResponse<MemberDTO> page(@RequestParam("page") Integer page,
                                 @RequestParam("pageSize") Integer pageSize,
                                 @RequestParam(value = "phone", required = false) String phone,
                                 @RequestParam(value = "status", required = false) Integer status);

    /**
     * 禁用会员（禁用后不可下单，USR-01）
     *
     * @param id 会员 id
     * @return
     */
    @PutMapping("/member/{id}/disable")
    Result disable(@PathVariable(name = "id") String id);

    /**
     * 启用会员
     *
     * @param id 会员 id
     * @return
     */
    @PutMapping("/member/{id}/enable")
    Result enable(@PathVariable(name = "id") String id);

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
