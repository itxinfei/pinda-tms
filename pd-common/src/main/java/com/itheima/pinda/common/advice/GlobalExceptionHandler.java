package com.itheima.pinda.common.advice;

import com.itheima.pinda.common.exception.PdException;
import com.itheima.pinda.common.utils.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import org.springframework.http.HttpStatus;

/**
 * 全局统一异常映射（《12》§8.2 / D-03）。
 *
 * <p>基包 {@code com.itheima.pinda} 的服务（四 BFF、dispatch、base、oms、work 等）
 * 经组件扫描自动生效，无需各服务自写 {@code GlobalExceptionHandler}。</p>
 *
 * <p>{@link Order#LOWEST_PRECEDENCE}：服务自有 advice（如 pd-netty 的
 * Same-Token/NotLogin 收口、customer 的会员身份处理）优先命中，
 * 本类只兜底未特化处理的公共异常。</p>
 *
 * <p>HTTP 语义约定（《66》D-03 修复方向）：参数/校验类 400、方法不支持 405、
 * 媒体类型 415、未分类 500；{@link PdException} 属业务异常，HTTP 200 +
 * 业务 code，与既有 {@code Result.error(code, msg)} 前端约定一致。</p>
 */
@Slf4j
@Order(Ordered.LOWEST_PRECEDENCE)
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：透传自带 code（默认 500），HTTP 200 走统一 Result 壳 */
    @ExceptionHandler(PdException.class)
    public Result handlePdException(PdException e) {
        log.warn("[业务异常] code={}, msg={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /** 参数校验失败（@Valid body / form binding）：400 + 首条字段错误 */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result handleValidation(BindException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String msg = fieldError == null ? "参数校验失败" : fieldError.getDefaultMessage();
        log.warn("[参数校验] 400: {}", msg);
        return Result.error(HttpStatus.BAD_REQUEST.value(), msg);
    }

    /** 缺必填参数 / 参数类型错误 / 请求体不可读：400 */
    @ExceptionHandler({MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result handleBadRequest(Exception e) {
        log.warn("[参数错误] 400: {}", e.getMessage());
        return Result.error(HttpStatus.BAD_REQUEST.value(), "请求参数错误");
    }

    /** 请求方法不支持：405（X-02 修复方向，替代 Spring 默认 500） */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("[方法不支持] 405: {}", e.getMessage());
        return Result.error(HttpStatus.METHOD_NOT_ALLOWED.value(), "请求方法不支持");
    }

    /** 媒体类型不支持：415 */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public Result handleMediaType(HttpMediaTypeNotSupportedException e) {
        log.warn("[媒体类型] 415: {}", e.getMessage());
        return Result.error(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), "不支持的请求媒体类型");
    }

    /** 兜底：未分类异常统一 500，不外露堆栈与内部信息 */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result handleException(Exception e) {
        log.error("[系统异常] 500: {}", e.getMessage(), e);
        return Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "系统繁忙，请稍后重试");
    }
}
