package com.itheima.pinda.controller;

import com.itheima.pinda.DTO.MemberDTO;
import com.itheima.pinda.common.utils.PageResponse;
import com.itheima.pinda.common.utils.Result;
import com.itheima.pinda.feign.MemberFeign;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端会员管理（USR-01）：薄转发至 pd-user，网关前缀 /api/manager。
 * 会员档案不做 VO 二次转换，直接透传共享契约 MemberDTO。
 */
@RestController
@RequestMapping("user-center/member")
@Tag(name = "会员管理")
public class MemberController {

    @Autowired
    private MemberFeign memberFeign;

    @Operation(summary = "会员分页查询")
    @GetMapping("/page")
    public PageResponse<MemberDTO> page(@RequestParam(name = "page") Integer page,
                                        @RequestParam(name = "pageSize") Integer pageSize,
                                        @RequestParam(name = "phone", required = false) String phone,
                                        @RequestParam(name = "status", required = false) Integer status) {
        return memberFeign.page(page, pageSize, phone, status);
    }

    @Operation(summary = "会员详情")
    @GetMapping("/detail/{id}")
    public MemberDTO detail(@PathVariable(name = "id") String id) {
        return memberFeign.detail(id);
    }

    @Operation(summary = "新增会员")
    @PostMapping("")
    public Result save(@RequestBody MemberDTO entity) {
        return memberFeign.save(entity);
    }

    @Operation(summary = "修改会员")
    @PutMapping("/{id}")
    public Result update(@PathVariable(name = "id") String id, @RequestBody MemberDTO entity) {
        return memberFeign.update(id, entity);
    }

    @Operation(summary = "删除会员（逻辑删除）")
    @DeleteMapping("/{id}")
    public Result del(@PathVariable(name = "id") String id) {
        return memberFeign.del(id);
    }

    @Operation(summary = "禁用会员")
    @PutMapping("/{id}/disable")
    public Result disable(@PathVariable(name = "id") String id) {
        return memberFeign.disable(id);
    }

    @Operation(summary = "启用会员")
    @PutMapping("/{id}/enable")
    public Result enable(@PathVariable(name = "id") String id) {
        return memberFeign.enable(id);
    }
}
