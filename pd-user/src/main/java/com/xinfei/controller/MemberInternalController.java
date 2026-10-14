package com.xinfei.controller;

import com.xinfei.DTO.MemberAutoCreateDTO;
import com.xinfei.DTO.MemberDTO;
import com.xinfei.converter.MemberConverter;
import com.xinfei.entity.Member;
import com.xinfei.service.IMemberService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 会员内部端点：仅供微服务间 Feign 调用（客户 BFF 解析会员身份）。
 *
 * <p>路径 {@code /member/internal/**} 预留统一 Same-Token 入站加固；网关无 pd-user 路由，
 * 外部不可达。租户隔离由 TenantLineInnerInterceptor 按 tenantId 请求头自动处理。</p>
 */
@Log4j2
@RestController
@RequestMapping("member/internal")
public class MemberInternalController {

    @Autowired
    private IMemberService memberService;

    @Autowired
    private MemberConverter memberConverter;

    /**
     * 按租户+手机号查会员
     */
    @GetMapping("/phone")
    public MemberDTO getByPhone(@RequestParam("phone") String phone) {
        if (StringUtils.isBlank(phone)) {
            return null;
        }
        return memberConverter.toDto(memberService.getByPhone(phone.trim()));
    }

    /**
     * 按手机号查会员，不存在则自动建档（uk_tenant_phone 兜底并发）。
     * 正常调用方（客户 BFF）已先校验认证账号手机号非空；入参非法时防御性返回 null。
     */
    @PostMapping("/get-or-create")
    public MemberDTO getOrCreate(@RequestBody MemberAutoCreateDTO dto) {
        if (dto == null || StringUtils.isBlank(dto.getPhone())) {
            log.warn("[会员内部端点] get-or-create 入参手机号为空: {}", dto);
            return null;
        }
        Member member = memberService.getOrCreate(dto.getPhone().trim(), dto.getName(), dto.getAvatar());
        return memberConverter.toDto(member);
    }
}
