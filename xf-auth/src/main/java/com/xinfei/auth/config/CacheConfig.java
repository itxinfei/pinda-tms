package com.xinfei.auth.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Cache 配置。
 *
 * <p>区划（dict_area，66.5 万行）与机构（sys_org）属于低频变更、高频读取的全局共享数据，
 * 各 BFF 经 AreaFeign / OrgFeign 跨服务反复查询。这里在提供方 xf-auth 加一层进程内只读缓存，
 * 实现「一次远程查、多次命中」，降低跨服务调用与数据库压力。</p>
 *
 * <p>选型：{@link ConcurrentMapCacheManager}（spring-context 自带，无需新增任何依赖）。
 * 取舍：进程内、无 TTL、应用重启即失效、多实例之间不共享（各实例各自回源一次）。
 * 对区划/机构这类变更极不频繁的数据完全够用；后续若需要跨实例共享或自动过期，
 * 可平滑替换为 RedisCacheManager / CaffeineCacheManager（依赖已在链路上，改动很小）。</p>
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /** 区划按 id 缓存 */
    public static final String CACHE_AREA = "area";
    /** 区划按行政区划编码缓存 */
    public static final String CACHE_AREA_CODE = "areaCode";
    /** 区划子级列表（按 parentId / ids 查询）缓存 */
    public static final String CACHE_AREA_CHILDREN = "areaChildren";
    /** 机构按 id 缓存 */
    public static final String CACHE_ORG = "org";
    /** 机构列表（多条件）缓存 */
    public static final String CACHE_ORG_LIST = "orgList";

    @Bean
    public CacheManager cacheManager() {
        // 无参构造：动态按需创建缓存（避免漏注册导致静默不命中）；
        // 默认按引用存储（store-by-value=false），缓存的只读 DTO 无后续可变写，无需序列化。
        return new ConcurrentMapCacheManager();
    }
}
