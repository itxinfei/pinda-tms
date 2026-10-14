package com.xinfei.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xinfei.entity.Member;
import com.xinfei.mapper.MemberMapper;
import com.xinfei.service.IMemberService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 用户服务类实现
 */
@Slf4j
@Service
public class MemberServiceImpl extends ServiceImpl<MemberMapper, Member> implements IMemberService {

    @Override
    public Member getByPhone(String phone) {
        if (StringUtils.isBlank(phone)) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<Member>()
                .eq(Member::getPhone, phone)
                .last("LIMIT 1"));
    }

    @Override
    public Member getOrCreate(String phone, String nickname, String avatar) {
        Member existing = getByPhone(phone);
        if (existing != null) {
            return existing;
        }
        LocalDateTime now = LocalDateTime.now();
        Member member = new Member();
        member.setPhone(phone);
        member.setNickname(nickname);
        member.setAvatar(avatar);
        // 新会员默认未实名、正常状态；id 走雪花 ASSIGN_ID，tenant_id 由多租户插件 insert 时补列
        member.setIdCardVerify(0);
        member.setStatus(1);
        member.setCreateTime(now);
        member.setUpdateTime(now);
        try {
            save(member);
            return member;
        } catch (DuplicateKeyException e) {
            // 同租户并发建档被 uk_tenant_phone 拦截：重查已落库的会员，不把冲突抛给调用方
            log.info("会员并发建档触发唯一键，重查既有会员: phone={}", phone);
            Member concurrent = getByPhone(phone);
            if (concurrent != null) {
                return concurrent;
            }
            throw e;
        }
    }
}
