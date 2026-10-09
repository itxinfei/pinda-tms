package com.itheima.pinda.common.utils;

import com.itheima.pinda.common.exception.PdException;
import lombok.extern.slf4j.Slf4j;

/**
 * 资源归属校验工具（防止水平越权）。
 * <p>
 * 采用 deny-by-default 原则：当前登录用户为空、资源无归属人或二者不一致，一律拒绝。
 * pd-web 三端没有 PdException 的全局异常处理器，故主入口 {@link #checkEquals}
 * 返回 code=403 的 Result，Controller 中直接 return 即可，避免校验逻辑到处复制。
 */
@Slf4j
public final class OwnershipAssert {

    /** 无权限对应的错误码（HTTP 403 语义） */
    public static final int FORBIDDEN = 403;

    private OwnershipAssert() {
    }

    /**
     * 校验资源归属人是否为当前登录用户。
     *
     * @param actualOwner 资源实际归属人（司机作业单 driverId、取派任务 courierId、订单 memberId 等）
     * @param currentUser 当前登录用户（RequestContext.getUserId()）
     * @param message     拒绝时返回给前端的提示信息
     * @return 归属一致返回 null（放行）；不一致返回 code=403 的 Result
     */
    public static Result checkEquals(String actualOwner, String currentUser, String message) {
        if (currentUser != null && actualOwner != null && currentUser.equals(actualOwner)) {
            return null;
        }
        log.warn("[越权拦截] 资源归属人={}, 当前用户={}, 提示={}", actualOwner, currentUser, message);
        return Result.error(FORBIDDEN, message);
    }

    /**
     * 断言资源归属，不匹配直接抛出 code=403 的运行时异常。
     * 供需要触发事务回滚、或方法签名不便于返回 Result 的场景使用。
     *
     * @param actualOwner 资源实际归属人
     * @param currentUser 当前登录用户
     * @param message     拒绝时的提示信息
     */
    public static void assertEquals(String actualOwner, String currentUser, String message) {
        Result deny = checkEquals(actualOwner, currentUser, message);
        if (deny != null) {
            throw new PdException(message, FORBIDDEN);
        }
    }
}
