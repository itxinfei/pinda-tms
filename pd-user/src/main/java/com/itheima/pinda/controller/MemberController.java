package com.itheima.pinda.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.pinda.DTO.MemberDTO;
import com.itheima.pinda.common.utils.PageResponse;
import com.itheima.pinda.common.utils.Result;
import com.itheima.pinda.converter.MemberConverter;
import com.itheima.pinda.entity.Member;
import com.itheima.pinda.service.IMemberService;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 用户前端控制器
 */
@Log4j2
@RestController
@RequestMapping("member")
public class MemberController {
    @Autowired
    private IMemberService memberService;

    @Autowired
    private MemberConverter memberConverter;

    /**
     * 新增
     *
     * @param entity
     * @return
     */
    @PostMapping("")
    public Result save(@RequestBody MemberDTO entity) {
        boolean result = memberService.save(memberConverter.toEntity(entity));
        if (result) {
            return Result.ok();
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
    public MemberDTO detail(@PathVariable(name = "id") String id) {
        Member member = memberService.getById(id);
        if (member == null) {
            return null;
        }
        return memberConverter.toDto(member);
    }

    /**
     * 分页查询
     *
     * @param page
     * @param pageSize
     * @return
     */
    @GetMapping("page")
    public PageResponse<MemberDTO> page(Integer page, Integer pageSize) {
        Page<Member> iPage = new Page(page, pageSize);
        LambdaQueryWrapper<Member> queryWrapper = new LambdaQueryWrapper<>();
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
     * 修改
     *
     * @param id
     * @param entity
     * @return
     */
    @PutMapping("/{id}")
    public Result update(@PathVariable(name = "id") String id, @RequestBody MemberDTO entity) {
        Member member = memberConverter.toEntity(entity);
        member.setId(id);
        boolean result = memberService.updateById(member);
        if (result) {
            return Result.ok();
        }
        return Result.error();
    }

    /**
     * 删除
     *
     * @param id
     * @return
     */
    @DeleteMapping("/{id}")
    public Result del(@PathVariable(name = "id") String id) {
        boolean result = memberService.removeById(id);
        if (result) {
            return Result.ok();
        }
        return Result.error();
    }
}
