package com.itheima.pinda.common.satoken;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 跨服务共享的 Sa-Token 权限/角色数据源（#9 草稿）。
 *
 * <p><b>设计</b>：pd-auth 登录时已把 {@code roles/perms} 写入 token-session（同一 Redis），
 * 下游 BFF（pd-web-manager 等）引入 sa-token-spring-boot3-starter + sa-token-redis-jackson、
 * {@code sa-token.token-name=token} 后，本实现直接从当前会话读取，<b>零查库、零 Feign</b>。</p>
 *
 * <p><b>与 pd-auth 的关系</b>：pd-auth 内有自己的 {@code StpInterfaceImpl}（查 sys_role_menu
 * 聚合，供登录写 session 与管理端实时校验）；本类供<b>不持有用户/菜单表的 BFF</b> 使用。
 * 同一应用若同时存在两个 StpInterface Bean 会冲突，故 BFF 只装配本类，pd-auth 不扫描本包。</p>
 *
 * <p><b>降级</b>：会话无 roles/perms（如旧 token、未登录）返回空列表，不抛异常；
 * 是否拒绝访问由网关登录态与 {@code @SaCheckPermission} 决定。</p>
 */
public class SharedSessionStpInterface implements StpInterface {

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return readStringList(SaSessionAuthKeys.PERMS);
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return readStringList(SaSessionAuthKeys.ROLES);
    }

    @SuppressWarnings("unchecked")
    private List<String> readStringList(String key) {
        try {
            if (!StpUtil.isLogin()) {
                return Collections.emptyList();
            }
            SaSession session = StpUtil.getTokenSession();
            Object value = session.get(key);
            if (value instanceof List<?> list) {
                List<String> result = new ArrayList<>(list.size());
                for (Object o : list) {
                    if (o != null) {
                        result.add(String.valueOf(o));
                    }
                }
                return result;
            }
            return Collections.emptyList();
        } catch (Exception e) {
            // 会话读取失败（Redis 抖动/旧 token）按"无权限"处理，交给上层鉴权拒绝，不连带 500
            return Collections.emptyList();
        }
    }
}
