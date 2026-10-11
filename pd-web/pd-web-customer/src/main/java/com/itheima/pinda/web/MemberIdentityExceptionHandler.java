package com.itheima.pinda.web;

import com.itheima.pinda.common.utils.Result;
import com.itheima.pinda.component.MemberIdentityException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 客户 BFF 异常处理：仅接管 {@link MemberIdentityException}，转统一 Result 响应。
 *
 * <p>其他异常维持各 Controller 既有 try/catch 行为，不在此扩大处理面（YAGNI）。</p>
 */
@Slf4j
@RestControllerAdvice
public class MemberIdentityExceptionHandler {

    /** 会员身份解析失败：按异常自带 code（400/401/403/500）回给前端 */
    @ExceptionHandler(MemberIdentityException.class)
    public Result handleMemberIdentity(MemberIdentityException e) {
        log.warn("[会员身份] 请求被拒绝: code={}, msg={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }
}
