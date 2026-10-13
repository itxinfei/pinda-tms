package com.itheima.pinda.common.satoken;

/**
 * Sa-Token token-session 中业务身份字段的统一键名（pd-auth 登录写入、各服务读取）。
 *
 * <p>跨服务共享：pd-auth 登录成功后把角色 code、权限码集合写入当前 token 的
 * SaSession（Redis 持久化）；下游 BFF 引入 sa-token + 同一 Redis 后，
 * {@link SharedSessionStpInterface} 直接读 session，无需反查库/Feign。</p>
 */
public final class SaSessionAuthKeys {

    private SaSessionAuthKeys() {
    }

    /** 租户ID（pd-auth 已在写，保持与 MybatisPlusConfigure.SESSION_TENANT_ID 同值） */
    public static final String TENANT_ID = "tenantId";

    /** 角色 code 集合，List&lt;String&gt; */
    public static final String ROLES = "roles";

    /** 权限码集合，List&lt;String&gt;；超管为 ["*"] */
    public static final String PERMS = "perms";

    /** 账号 */
    public static final String ACCOUNT = "account";

    /** 姓名 */
    public static final String NAME = "name";

    /** 机构ID */
    public static final String ORG_ID = "orgId";

    /** 超管角色 code */
    public static final String SUPER_ROLE = "ADMIN";

    /** 全权限通配 */
    public static final String ALL_PERMISSION = "*";
}
