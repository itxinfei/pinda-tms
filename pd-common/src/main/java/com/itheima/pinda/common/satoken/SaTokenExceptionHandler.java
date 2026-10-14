package com.itheima.pinda.common.satoken;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotRoleException;
import com.itheima.pinda.common.utils.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Sa-Token 鉴权异常统一收口（X-01 下沉落点的配套，避免 401/403 落进公共兜底变 500）。
 *
 * <p>仅当服务配置 {@code pinda.auth.required-roles}（启用 {@link RequiredRoleInterceptor}）
 * 时由 {@link SaTokenPermissionAutoConfiguration} 装配；pd-auth 等未配置该属性的服务
 * 不装配本类，维持其自有异常处理。</p>
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class SaTokenExceptionHandler {

    /** 未登录（无 token 访问受角色保护的本端）：401 */
    @ExceptionHandler(NotLoginException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result handleNotLogin(NotLoginException e) {
        return Result.error(HttpStatus.UNAUTHORIZED.value(), "未登录或登录已过期，请重新登录");
    }

    /** 已登录但角色不符（端级角色绑定拒绝跨端 token）：403 */
    @ExceptionHandler(NotRoleException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result handleNotRole(NotRoleException e) {
        log.warn("[端级鉴权] 角色不符: {}", e.getMessage());
        return Result.error(HttpStatus.FORBIDDEN.value(), "无权访问该端功能");
    }
}
