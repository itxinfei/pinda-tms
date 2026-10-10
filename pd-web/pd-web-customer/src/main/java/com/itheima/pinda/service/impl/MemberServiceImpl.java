package com.itheima.pinda.service.impl;

import com.itheima.pinda.DTO.MemberDTO;
import com.itheima.pinda.DTO.UserDTO;
import com.itheima.pinda.common.utils.Result;
import com.itheima.pinda.feign.MemberFeign;
import com.itheima.pinda.feign.UserClient;
import com.itheima.pinda.service.IMemberService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MemberServiceImpl implements IMemberService {

    @Autowired
    private MemberFeign memberFeign;

    @Autowired
    private UserClient userClient;


    @Override
    public MemberDTO detail(String userId) {
        log.info("查找用户信息(本地):{}", userId);
        MemberDTO member = memberFeign.detail(userId);
        log.info("查找用户信息(本地):{} Result:{}", userId, member);

        log.info("查找用户信息(远端):{}", userId);
        UserDTO user = userClient.findById(Long.valueOf(userId));
        log.info("查找用户信息(远端):{} Result:{}", userId, user);

        if (member == null) {
            // 本地不存在  创建一份
            if (user != null) {
                member = new MemberDTO();
                member.setId(userId);
                member.setAuthId(userId);
                member.setPhone(user.getMobile());
                member.setAvatar(user.getAvatar());
                member.setName(user.getName());
                Result result = memberFeign.save(member);
                log.info("查找用户信息(远端):{} 留存结果:{}", userId, result);
            }
        } else {
            if (user != null) {
                member.setAvatar(user.getAvatar());
                member.setName(user.getName());
            }
        }

        return member;
    }

}
