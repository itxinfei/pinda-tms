package com.itheima.pinda.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.pinda.DTO.MemberDTO;
import com.itheima.pinda.controller.support.IdConverter;
import com.itheima.pinda.common.utils.Constant;
import com.itheima.pinda.common.utils.PageResponse;
import com.itheima.pinda.common.utils.Result;
import com.itheima.pinda.converter.MemberConverter;
import com.itheima.pinda.entity.Member;
import com.itheima.pinda.service.IMemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.*;

/**
 * 会员前端控制器（USR-01：档案 CRUD + 状态启停 + 分页筛选）
 */
@Log4j2
@RestController
@RequestMapping("member")
@Tag(name = "会员管理")
public class MemberController {
    @Autowired
    private IMemberService memberService;

    @Autowired
    private MemberConverter memberConverter;

    /**
     * 新增（管理端建档）。手机号为租户内唯一键，重复返回 400；新会员默认正常状态。
     *
     * @param entity
     * @return
     */
    @PostMapping("")
    @Operation(summary = "新增会员")
    public Result save(@RequestBody MemberDTO entity) {
        if (StringUtils.isBlank(entity.getPhone())) {
            return Result.error(400, "手机号不能为空");
        }
        if (memberService.getByPhone(entity.getPhone().trim()) != null) {
            return Result.error(400, "该手机号已注册会员");
        }
        Member member = memberConverter.toEntity(entity);
        // 建档统一为正常状态；启停只允许走 disable/enable 端点
        member.setStatus(Constant.DATA_DEFAULT_STATUS);
        try {
            boolean result = memberService.save(member);
            if (result) {
                return Result.ok();
            }
        } catch (DuplicateKeyException e) {
            // 预检查查不到但唯一键仍冲突：手机号会员此前被逻辑删除，deleted 行仍占用 uk_tenant_phone
            log.warn("会员建档撞唯一键（含逻辑删除行占用）: phone={}", entity.getPhone());
            return Result.error(400, "该手机号已注册会员");
        }
        return Result.error();
    }

    /**
     * 详情（不存在返回 null，HTTP 200；消费方自行判空）
     *
     * @param id
     * @return
     */
    @GetMapping("detail/{id}")
    @Operation(summary = "根据id查询会员详情")
    public MemberDTO detail(@PathVariable(name = "id") String id) {
        Member member = memberService.getById(IdConverter.toLong(id));
        if (member == null) {
            return null;
        }
        return memberConverter.toDto(member);
    }

    /**
     * 分页查询
     *
     * @param page     页码
     * @param pageSize 每页条数
     * @param phone    手机号（模糊匹配，可空）
     * @param status   状态：1 正常 0 禁用，不传查全部
     * @return
     */
    @GetMapping("page")
    @Operation(summary = "分页查询会员")
    public PageResponse<MemberDTO> page(Integer page, Integer pageSize,
                                        @RequestParam(name = "phone", required = false) String phone,
                                        @RequestParam(name = "status", required = false) Integer status) {
        Page<Member> iPage = new Page(page, pageSize);
        LambdaQueryWrapper<Member> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(phone)) {
            queryWrapper.like(Member::getPhone, phone.trim());
        }
        if (status != null) {
            queryWrapper.eq(Member::getStatus, status);
        }
        queryWrapper.orderByDesc(Member::getCreateTime);
        Page<Member> pageResult = memberService.page(iPage, queryWrapper);

        return PageResponse.<MemberDTO>builder()
                .items(memberConverter.toDtoList(pageResult.getRecords()))
                .page(page)
                .pagesize(pageSize)
                .pages(pageResult.getPages())
                .counts(pageResult.getTotal())
                .build();
    }

    /**
     * 修改（仅昵称/头像/身份证号等档案字段；status 由启停端点单独维护）
     *
     * @param id
     * @param entity
     * @return
     */
    @PutMapping("/{id}")
    @Operation(summary = "修改会员")
    public Result update(@PathVariable(name = "id") String id, @RequestBody MemberDTO entity) {
        Member member = memberConverter.toEntity(entity);
        member.setId(IdConverter.toLong(id));
        boolean result = memberService.updateById(member);
        if (result) {
            return Result.ok();
        }
        return Result.error();
    }

    /**
     * 删除（逻辑删除 deleted=1）
     *
     * @param id
     * @return
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除会员")
    public Result del(@PathVariable(name = "id") String id) {
        boolean result = memberService.removeById(IdConverter.toLong(id));
        if (result) {
            return Result.ok();
        }
        return Result.error();
    }

    /**
     * 禁用会员：禁用后该会员在客户端下单/预估价被拒（USR-01），历史订单不受影响。
     *
     * @param id 会员 id
     * @return
     */
    @PutMapping("/{id}/disable")
    @Operation(summary = "禁用会员")
    public Result disable(@PathVariable(name = "id") String id) {
        Member member = new Member();
        member.setId(IdConverter.toLong(id));
        member.setStatus(Constant.DATA_DISABLE_STATUS);
        boolean result = memberService.updateById(member);
        return result ? Result.ok() : Result.error();
    }

    /**
     * 启用会员（与禁用对称，恢复下单资格）
     *
     * @param id 会员 id
     * @return
     */
    @PutMapping("/{id}/enable")
    @Operation(summary = "启用会员")
    public Result enable(@PathVariable(name = "id") String id) {
        Member member = new Member();
        member.setId(IdConverter.toLong(id));
        member.setStatus(Constant.DATA_DEFAULT_STATUS);
        boolean result = memberService.updateById(member);
        return result ? Result.ok() : Result.error();
    }
}
