package com.itheima.auth.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应结构。成功 code=0，失败为具体错误码。
 */
@Data
public class R<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 成功码 */
    public static final int SUCCESS_CODE = 0;
    /** 通用业务失败码 */
    public static final int FAIL_CODE = 400;
    /** 未登录 */
    public static final int UNAUTHORIZED_CODE = 401;

    private int code;
    private String msg;
    private T data;

    public static <T> R<T> success() {
        return success(null);
    }

    public static <T> R<T> success(T data) {
        R<T> r = new R<>();
        r.setCode(SUCCESS_CODE);
        r.setMsg("ok");
        r.setData(data);
        return r;
    }

    public static <T> R<T> fail(String msg) {
        return fail(FAIL_CODE, msg);
    }

    public static <T> R<T> fail(int code, String msg) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setMsg(msg);
        return r;
    }
}
