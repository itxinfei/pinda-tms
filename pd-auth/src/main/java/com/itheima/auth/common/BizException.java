package com.itheima.auth.common;

import lombok.Getter;

/**
 * 业务异常：携带错误码与提示信息，由全局异常处理器统一转换为 R。
 */
@Getter
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;

    public BizException(String msg) {
        this(R.FAIL_CODE, msg);
    }

    public BizException(int code, String msg) {
        super(msg);
        this.code = code;
    }

    /** 参数/校验类失败的快捷构造 */
    public static BizException validFail(String msg) {
        return new BizException(R.FAIL_CODE, msg);
    }
}
