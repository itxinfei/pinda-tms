package com.xinfei.auth.common.v1;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.SameTokenInvalidException;
import com.xinfei.auth.common.BizException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * /api/v1（youlai 契约）专用异常处理：仅作用于 controller.v1 包，
 * 优先于全局 {@code GlobalExceptionHandler}（后者返回心飞数字 code 的 R）。
 * 统一返回字符串 code 的 {@link ApiResult}：成功 00000、未登录 A0230、无权限 A0301。
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.xinfei.auth.controller.v1")
public class V1ExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ApiResult<Void> handleBiz(BizException e) {
        log.warn("[v1] 业务异常: {}", e.getMessage());
        return ApiResult.fail(e.getMessage());
    }

    /** 用户导入存在无效行：返回结构化 ExcelResult（HTTP 200，前端按 invalidCount 提示） */
    @ExceptionHandler(com.xinfei.auth.service.v1.UserService.ImportRowsException.class)
    public ApiResult<com.xinfei.auth.common.v1.ExcelResult> handleImportRows(
            com.xinfei.auth.service.v1.UserService.ImportRowsException e) {
        return ApiResult.success(e.getResult());
    }

    /** @RequestBody @Valid 校验失败 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResult<Void> handleValid(MethodArgumentNotValidException e) {
        FieldError fe = e.getBindingResult().getFieldError();
        return ApiResult.fail(fe != null ? fe.getDefaultMessage() : "参数校验失败");
    }

    /** 表单绑定校验失败 */
    @ExceptionHandler(BindException.class)
    public ApiResult<Void> handleBind(BindException e) {
        FieldError fe = e.getBindingResult().getFieldError();
        return ApiResult.fail(fe != null ? fe.getDefaultMessage() : "参数校验失败");
    }

    /** 方法参数级约束失败 */
    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResult<Void> handleConstraint(ConstraintViolationException e) {
        return ApiResult.fail(e.getMessage());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ApiResult<Void> handleMissingParam(MissingServletRequestParameterException e) {
        return ApiResult.fail("缺少必需参数：" + e.getParameterName());
    }

    /** Sa-Token 未登录 */
    @ExceptionHandler(NotLoginException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResult<Void> handleNotLogin(NotLoginException e) {
        return ApiResult.fail(ApiResult.UNAUTHORIZED, "未登录或登录已过期，请重新登录");
    }

    /** Sa-Token 权限码不足（@SaCheckPermission） */
    @ExceptionHandler(NotPermissionException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResult<Void> handleNoPermission(NotPermissionException e) {
        return ApiResult.fail(ApiResult.FORBIDDEN, "权限不足");
    }

    @ExceptionHandler(SameTokenInvalidException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResult<Void> handleSameToken(SameTokenInvalidException e) {
        return ApiResult.fail(ApiResult.UNAUTHORIZED, "非法来源请求，请通过网关访问");
    }

    /** 兜底 */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResult<Void> handleException(Exception e) {
        log.error("[v1] 系统异常", e);
        return ApiResult.fail("系统繁忙，请稍后再试");
    }
}
