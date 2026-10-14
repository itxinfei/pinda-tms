package com.xinfei.service.impl;

import com.xinfei.DTO.MemberDTO;
import com.xinfei.component.MemberIdentityResolver;
import com.xinfei.service.IMemberService;
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
