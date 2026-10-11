package com.itheima.pinda.component;

/**
 * 会员身份解析失败异常：客户 BFF 无法把登录的认证账号（sys_user.id）
 * 解析为会员（member_info.id）——未登录/账号异常/未绑手机号/会员服务故障。
 *
 * <p>由 {@code MemberIdentityExceptionHandler} 统一转 Result，业务调用点直接抛出即可，
 * 不必在每个 Controller 重复判空。</p>
 */
public class MemberIdentityException extends RuntimeException {

    /** 401 未登录或账号异常；400 账号未绑定手机号；403 会员已被禁用；500 会员服务异常 */
    private final int code;

    public MemberIdentityException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
