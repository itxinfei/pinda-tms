package com.itheima.gateway.config;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.reactor.context.SaReactorSyncHolder;
import cn.dev33.satoken.reactor.filter.SaReactorFilter;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;

/**
 * 网关统一鉴权：匿名白名单放行，其余路径必须持有有效登录 token。
 *
 * 说明（Sa-Token v1.46 Reactor 模型）：
 *   - setAuth 的入参为 null，当前 exchange 要从 SaReactorSyncHolder 取；
 *   - setError 的返回字符串会被 writeResult 以 text/plain 写回，
 *     故这里自行设置 JSON Content-Type 与 HTTP 状态码。
 */
@Configuration
public class SaTokenConfigure {

    @Bean
    public SaReactorFilter saReactorFilter() {
        return new SaReactorFilter()
                .addInclude("/**")
                // 排除：favicon + Actuator 健康探针。网关是 WebFlux，SaReactorFilter 作为全局 WebFilter
                // 对 /actuator/** 同样生效（区别于 MVC 业务拦截器默认不拦 Actuator 端点），若不显式排除，
                // 容器内 healthcheck 无 token 探活 /actuator/health 会被打成 401，导致网关恒为 unhealthy。
                // 仅放行 health（management 默认只暴露该端点，无库、仅进程级存活，不含敏感信息）。
                .addExclude("/favicon.ico", "/actuator/health", "/actuator/health/**")
                .setAuth(obj -> {
                    ServerWebExchange exchange = SaReactorSyncHolder.getExchange();
                    // CORS 预检请求放行（正常情况下已由 CorsWebFilter 处理，双保险）
                    if (exchange.getRequest().getMethod() != null
                            && "OPTIONS".equalsIgnoreCase(exchange.getRequest().getMethod().name())) {
                        return;
                    }
                    // 白名单：认证服务匿名接口（验证码 / 登录）
                    SaRouter.match("/api/auth/anno/**").stop();
                    // 其余路径一律要求登录
                    SaRouter.match("/**", r -> StpUtil.checkLogin());
                    // 网关只做登录态校验（认证 Authentication），不在此做角色鉴权（Authorization）：
                    // pd-gateway 不依赖 pd-common，classpath 没有 SharedSessionStpInterface，此处若调
                    // StpUtil.checkRole(...) 会落到 Sa-Token 默认空实现（角色恒为空），把所有用户
                    // （含 ADMIN）一律打成 403、后台全锁。X-01 各端角色绑定必须下沉到有角色数据的下游：
                    // pd-auth 的 StpInterfaceImpl（查库）与各 pd-web-* BFF（SharedSessionStpInterface 读会话）。
                })
                .setError(e -> {
                    ServerWebExchange exchange = SaReactorSyncHolder.getExchange();
                    ServerHttpResponse response = exchange.getResponse();
                    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

                    SaResult body = new SaResult();
                    if (e instanceof NotLoginException) {
                        response.setStatusCode(HttpStatus.UNAUTHORIZED);
                        body.setCode(401);
                        body.setMsg("未登录或登录已过期，请重新登录");
                    } else {
                        response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                        body.setCode(500);
                        body.setMsg(e.getMessage());
                    }
                    return SaManager.getSaJsonTemplate().objectToJson(body);
                });
    }
}
