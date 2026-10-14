package com.xinfei.common.satoken;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotRoleException;
import com.xinfei.common.utils.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Sa-Token 鉴权异常统一收口（X-01 下沉落点的配套，避免 401/403 落进公共兜底变 500）。
 *
 * <p>本类位于 {@code com.xinfei} 扫描路径内，会被无 sa-token 依赖的服务
 * （pd-base/oms/work/dispatch/user 等）组件扫描命中——必须类级条件门控：
 * {@code @ConditionalOnClass(name=...)} 用字符串 ASM 判定（不写 class 字面量，
 * 否则条件判定本身就要加载缺失类），{@code @ConditionalOnProperty} 保证仅在
 * 配置了 {@code pinda.auth.required-roles} 的端生效。</p>
 *
 * <p>注意：不能用 @Bean 方法注册本类——BFF 既扫到类又走 @Bean 会产生重复
 * Bean 定义导致 BeanDefinitionOverrideException。</p>
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnClass(name = "cn.dev33.satoken.exception.NotRoleException")
@ConditionalOnProperty(name = "pinda.auth.required-roles")
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
