package com.itheima.pinda.tenant;

import com.itheima.pinda.feign.OrgFeign;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统态租户解析器：Quartz 定时任务没有登录态与请求头，执行前按机构ID反查其租户ID。
 *
 * <p>机构归属基本不变，结果以 {@link ConcurrentHashMap} 做进程内缓存，避免引入额外缓存中间件；
 * 远程失败的结果不缓存，下一轮可重新解析。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrgTenantResolver {

    private final OrgFeign orgFeign;

    private final Map<Long, Long> tenantCache = new ConcurrentHashMap<>();

    /**
     * 按机构ID解析租户ID。
     *
     * @param orgId 机构ID
     * @return 租户ID；机构ID为空、远程失败或查无数据时返回 0（与 pd-auth 安全回落一致，不泄露租户数据）
     */
    public Long resolve(Long orgId) {
        if (orgId == null) {
            return 0L;
        }
        Long cached = tenantCache.get(orgId);
        if (cached != null) {
            return cached;
        }
        try {
            Long tenantId = orgFeign.getTenantIdByOrgId(orgId);
            if (tenantId != null) {
                tenantCache.put(orgId, tenantId);
                log.info("系统态机构[{}]解析出租户[{}]", orgId, tenantId);
                return tenantId;
            }
        } catch (Exception e) {
            log.error("系统态机构[{}]解析租户失败，本轮回落租户0", orgId, e);
        }
        return 0L;
    }
}
