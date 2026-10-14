package com.xinfei.controller;


import com.xinfei.DTO.MemberDTO;
import com.xinfei.DTO.UserProfileDTO;
import com.xinfei.common.utils.Result;
import com.xinfei.service.IMemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 客户个人信息
 */
@Slf4j
@Tag(name = "用户管理")
@Controller
@RequestMapping("user")
public class UserController {

    private final IMemberService memberService;

    public UserController(IMemberService memberService) {
        this.memberService = memberService;
    }

    @SneakyThrows
    @Operation(summary = "我的信息")
    @ResponseBody
    @GetMapping("profile")
    public Result profile() {
        // 未登录/未绑手机号等身份异常由 MemberIdentityExceptionHandler 统一转 Result
        MemberDTO member = memberService.currentMember();
        // 返回的 id 是会员雪花 ID（member_info.id），不是认证账号 ID
        return Result.ok().put("data", UserProfileDTO.builder()
                .id(member.getId())
                .avatar(member.getAvatar())
                .name(member.getName())
                .phone(member.getPhone())
                .build());
    }
}
