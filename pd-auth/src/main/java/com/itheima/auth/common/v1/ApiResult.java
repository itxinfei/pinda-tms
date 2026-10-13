package com.itheima.auth.common.v1;

import lombok.Data;

import java.io.Serializable;

/**
 * youlai 契约统一响应（/api/v1/**，经网关 StripPrefix=2 后 pd-auth 侧路径）。
 *
 * <p>成功码固定字符串 {@code "00000"}（前端 vue3-element-admin 拦截器认该码，
 * 同时兼容数字 0）；与品达自有 {@code auth.common.R}（数字 code）分开，避免污染
 * Feign 内部契约。错误码沿用 youlai 前缀：A0230 未登录、A0301 权限不足。</p>
 */
@Data
public class ApiResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 成功 */
    public static final String SUCCESS = "00000";
    /** 通用业务失败 */
    public static final String FAIL = "B0000";
    /** 访问令牌无效/未登录 */
    public static final String UNAUTHORIZED = "A0230";
    /** 权限不足 */
    public static final String FORBIDDEN = "A0301";

    private String code;
    private String msg;
    private T data;

    public static <T> ApiResult<T> success() {
        return success(null);
    }

    public static <T> ApiResult<T> success(T data) {
        ApiResult<T> r = new ApiResult<>();
        r.setCode(SUCCESS);
        r.setMsg("ok");
        r.setData(data);
        return r;
    }

    public static <T> ApiResult<T> fail(String msg) {
        return fail(FAIL, msg);
    }

    public static <T> ApiResult<T> fail(String code, String msg) {
        ApiResult<T> r = new ApiResult<>();
        r.setCode(code);
        r.setMsg(msg);
        return r;
    }
}
