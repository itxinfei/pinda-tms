package com.xinfei.component;

import com.xinfei.DTO.MemberAutoCreateDTO;
import com.xinfei.DTO.MemberDTO;
import com.xinfei.DTO.UserDTO;
import com.xinfei.common.context.RequestContext;
import com.xinfei.feign.MemberFeign;
import com.xinfei.feign.UserClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 会员身份解析器：把网关注入的认证账号 ID（userid 请求头 = sys_user.id）
 * 解析为会员雪花 ID（member_info.id）。
 *
 * <p>解析链路：authUserId → pd-auth {@code /internal/user/{id}} 查账号取手机号 →
 * pd-user {@code /member/internal/get-or-create} 按 (租户,手机号) 查建会员。
 * 结果在单次 HTTP 请求内缓存（request attribute），同一次请求里下单/地址簿/越权校验
 * 等多个调用只解析一次。</p>
 *
 * <p>租户不由本组件处理：Feign 出站拦截器已透传 tenantId 头，pd-auth/pd-user 各自
 * 按租户隔离。解析失败抛 {@link MemberIdentityException}，由全局 advice 统一转 Result。</p>
 */
@Slf4j
@Component
public class MemberIdentityResolver {

    /** 请求级缓存键：会员雪花 ID 字符串 */
    private static final String REQ_ATTR_MEMBER_ID = "xinfei.customer.memberId";
    /** 请求级缓存键：会员完整信息 */
    private static final String REQ_ATTR_MEMBER = "xinfei.customer.member";

    private final UserClient userClient;
    private final MemberFeign memberFeign;

    public MemberIdentityResolver(UserClient userClient, MemberFeign memberFeign) {
        this.userClient = userClient;
        this.memberFeign = memberFeign;
    }

    /**
     * 获取当前登录用户的会员雪花 ID（String 形态，遵守雪花 ID 跨服务/下发一律 String 的铁律）。
     */
    public String requireCurrentMemberId() {
        return requireCurrentMember().getId();
    }

    /**
     * 获取当前会员并校验为正常状态（USR-01：禁用会员不可下单，下单/预估价入口调用）。
     *
     * @return 状态正常的当前会员
     * @throws MemberIdentityException status=0 时以 403 拒绝
     */
    public MemberDTO requireActiveMember() {
        MemberDTO member = requireCurrentMember();
        if (member.getStatus() != null && Integer.valueOf(0).equals(member.getStatus())) {
            log.warn("[会员身份] 会员已被禁用，拒绝下单: memberId={}", member.getId());
            throw new MemberIdentityException(403, "账号已被禁用，无法下单，请联系客服");
        }
        return member;
    }

    /**
     * 获取当前登录用户对应的会员完整信息（不存在则按手机号自动建档），请求级缓存。
     */
    public MemberDTO requireCurrentMember() {
        ServletRequestAttributes attributes = currentRequestAttributes();
        Object cached = attributes.getAttribute(REQ_ATTR_MEMBER, ServletRequestAttributes.SCOPE_REQUEST);
        if (cached instanceof MemberDTO member) {
            return member;
        }
        MemberDTO resolved = resolve();
        attributes.setAttribute(REQ_ATTR_MEMBER, resolved, ServletRequestAttributes.SCOPE_REQUEST);
        attributes.setAttribute(REQ_ATTR_MEMBER_ID, resolved.getId(), ServletRequestAttributes.SCOPE_REQUEST);
        return resolved;
    }

    /**
     * 执行一次完整解析：认证账号 → 手机号 → 会员 get-or-create。
     */
    private MemberDTO resolve() {
        String authUserId = RequestContext.getUserId();
        if (StringUtils.isBlank(authUserId)) {
            // 正常不会到这：网关对非白名单请求强制登录；此处为防御
            throw new MemberIdentityException(401, "未登录或登录已过期");
        }
        Long authId;
        try {
            authId = Long.valueOf(authUserId.trim());
        } catch (NumberFormatException e) {
            log.warn("[会员身份] 认证账号ID非法: authUserId={}", authUserId);
            throw new MemberIdentityException(401, "登录身份异常，请重新登录");
        }
        UserDTO user = userClient.findById(authId);
        if (user == null) {
            log.warn("[会员身份] 认证账号不存在: authUserId={}", authUserId);
            throw new MemberIdentityException(401, "登录账号不存在或已停用");
        }
        if (StringUtils.isBlank(user.getMobile())) {
            // 会员以手机号为租户内唯一键（member_info.uk_tenant_phone），无手机号无法建档
            log.warn("[会员身份] 认证账号未绑定手机号: authUserId={}", authUserId);
            throw new MemberIdentityException(400, "当前账号未绑定手机号，请先在个人中心绑定");
        }
        MemberAutoCreateDTO createDTO = new MemberAutoCreateDTO();
        createDTO.setPhone(user.getMobile().trim());
        createDTO.setName(user.getName());
        createDTO.setAvatar(user.getAvatar());
        MemberDTO member = memberFeign.getOrCreate(createDTO);
        if (member == null || StringUtils.isBlank(member.getId())) {
            log.error("[会员身份] 会员解析失败: authUserId={}, mobile={}", authUserId, user.getMobile());
            throw new MemberIdentityException(500, "会员信息初始化失败，请稍后重试");
        }
        return member;
    }

    private ServletRequestAttributes currentRequestAttributes() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            // BFF 入口全部是 HTTP 请求；非 HTTP 线程调用属于代码缺陷
            throw new MemberIdentityException(500, "缺少请求上下文，无法解析会员身份");
        }
        return attributes;
    }
}
