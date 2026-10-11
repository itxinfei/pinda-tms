package com.itheima.pinda.service.impl;

import com.itheima.pinda.DTO.MemberDTO;
import com.itheima.pinda.component.MemberIdentityResolver;
import com.itheima.pinda.service.IMemberService;
import org.springframework.stereotype.Service;

@Service
public class MemberServiceImpl implements IMemberService {

    private final MemberIdentityResolver memberIdentityResolver;

    public MemberServiceImpl(MemberIdentityResolver memberIdentityResolver) {
        this.memberIdentityResolver = memberIdentityResolver;
    }

    @Override
    public MemberDTO currentMember() {
        return memberIdentityResolver.requireCurrentMember();
    }
}
