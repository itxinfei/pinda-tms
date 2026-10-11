package com.itheima.pinda.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.itheima.pinda.entity.Member;

/**
 * 用户服务接口
 */
public interface IMemberService extends IService<Member> {

    /**
     * 按手机号查当前租户会员（租户条件由 TenantLineInnerInterceptor 自动追加）。
     *
     * @param phone 手机号；空白返回 null
     */
    Member getByPhone(String phone);

    /**
     * 按手机号查会员，不存在则自动建档；并发时靠 uk_tenant_phone 唯一键兜底重查。
     *
     * @param phone    手机号（必填）
     * @param nickname 昵称（可空）
     * @param avatar   头像（可空）
     */
    Member getOrCreate(String phone, String nickname, String avatar);
}
