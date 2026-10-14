package com.xinfei.service;

import com.xinfei.DTO.MemberDTO;

/**
 * 客户 BFF 会员视图服务。
 *
 * <p>会员身份以 member_info 雪花 ID 为准（不再把认证账号 ID 当会员 ID）：
 * 每次请求由 MemberIdentityResolver 解析 authUserId → 手机号 → 会员。</p>
 */
public interface IMemberService {

    /**
     * 获取当前登录用户对应的会员完整信息（不存在则按认证账号手机号自动建档）。
     * 解析失败抛 MemberIdentityException，由全局 advice 统一转 Result。
     */
    MemberDTO currentMember();
}
