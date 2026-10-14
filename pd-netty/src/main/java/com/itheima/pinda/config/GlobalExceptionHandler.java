package com.itheima.pinda.config;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.SameTokenInvalidException;
import com.itheima.pinda.common.utils.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理（2026-10-11）。
 *
 * <p>pd-netty 接入 Sa-Token 后，绕过网关直连 8192、或匿名访问受保护接口，
 * 若不处理会冒泡成 Tomcat 默认 500 白页。此处与 pd-auth 同一姿势，
 * 把 Same-Token / 未登录异常收口为 HTTP 401 + 统一 {@link Result} JSON，
 * 其余未捕获异常返回 500 JSON 且不外露堆栈。</p>
 *
 * <p>服务特有 advice 显式最高优先级，压过 pd-common 公共兜底
 * （{@code com.itheima.pinda.common.advice.GlobalExceptionHandler}）。</p>
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 未登录或登录已过期（受保护 HTTP 端点被匿名访问） */
    @ExceptionHandler(NotLoginException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result handleNotLogin(NotLoginException e) {
        return Result.error(HttpStatus.UNAUTHORIZED.value(), "未登录或登录已过期，请重新登录");
    }

    /** Same-Token 无效：请求未经网关转发，属于绕过网关直连子服务 */
    @ExceptionHandler(SameTokenInvalidException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result handleSameTokenInvalid(SameTokenInvalidException e) {
        return Result.error(HttpStatus.UNAUTHORIZED.value(), "非法来源请求，请通过网关访问");
    }

    /** 兜底：其余未捕获异常返回 500 JSON，堆栈仅落日志 */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result handleException(Exception e) {
        log.error("pd-netty 未捕获异常", e);
        return Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "系统繁忙，请稍后再试");
    }
}
