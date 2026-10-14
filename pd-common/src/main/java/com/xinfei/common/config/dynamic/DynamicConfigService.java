package com.xinfei.common.config.dynamic;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态参数配置读取服务（CFG-01），供业务服务在运行期热读取第三方凭据 / 可调参数。
 *
 * <p>读取优先级（每个 key 独立缓存，TTL 由 {@code xinfei.config.cache-ttl-seconds} 控制，默认 30s）：</p>
 * <ol>
 *   <li>内存缓存未过期直接返回；</li>
 *   <li>直读同库 {@code sys_config}（平台全局 {@code tenant_id=0} 且 {@code status=1}），
 *       敏感值解密；库中无启用项或值为空则回落；</li>
 *   <li>Spring {@link Environment}（yml / 环境变量 / {@code -D} 启动参数）。</li>
 * </ol>
 *
 * <p>不依赖 Redis / MQ：pd-oms 等未引 redis 的服务也可用。后台修改后最长 TTL 秒数全网生效；
 * 第三方凭据调用失败（如 AK 失效）时业务侧应调用 {@link #evict(String)} 立即丢弃缓存重读。
 * 数据库不可用时告警并回落 Environment，不阻断业务。</p>
 */
@Slf4j
public class DynamicConfigService {

    /** 平台全局配置租户 ID（sys_config 一期只使用 0） */
    private static final long PLATFORM_TENANT_ID = 0L;

    private static final String SELECT_ENABLED_SQL =
            "SELECT config_value, is_sensitive FROM sys_config "
                    + "WHERE tenant_id = ? AND config_key = ? AND status = 1 LIMIT 1";

    private final ObjectProvider<DataSource> dataSourceProvider;
    private final Environment environment;
    private final ConfigCrypto crypto;
    private final long ttlMillis;

    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public DynamicConfigService(ObjectProvider<DataSource> dataSourceProvider,
                                Environment environment,
                                ConfigCrypto crypto,
                                long cacheTtlSeconds) {
        this.dataSourceProvider = dataSourceProvider;
        this.environment = environment;
        this.crypto = crypto;
        this.ttlMillis = Math.max(1L, cacheTtlSeconds) * 1000L;
    }

    /**
     * 获取字符串配置（库 → Environment），结果可能为 {@code null}（两处均无配置）。
     */
    public String get(String key) {
        long now = System.currentTimeMillis();
        CacheEntry hit = cache.get(key);
        if (hit != null && now < hit.expireAt()) {
            return hit.value();
        }
        String resolved = resolve(key);
        cache.put(key, new CacheEntry(resolved, now + ttlMillis));
        return resolved;
    }

    /** 获取字符串配置，无配置时返回默认值 */
    public String get(String key, String defaultValue) {
        String value = get(key);
        return value == null ? defaultValue : value;
    }

    public int getInt(String key, int defaultValue) {
        String value = get(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            log.warn("配置项 {} 不是合法整数，值={}，使用默认值 {}", key, value, defaultValue);
            return defaultValue;
        }
    }

    public long getLong(String key, long defaultValue) {
        String value = get(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            log.warn("配置项 {} 不是合法长整数，值={}，使用默认值 {}", key, value, defaultValue);
            return defaultValue;
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        String v = value.trim().toLowerCase();
        return "true".equals(v) || "1".equals(v) || "yes".equals(v) || "y".equals(v);
    }

    /** 丢弃指定 key 的本地缓存，下次读取强制回源（第三方凭据失效时调用） */
    public void evict(String key) {
        cache.remove(key);
    }

    /** 清空全部本地缓存 */
    public void evictAll() {
        cache.clear();
    }

    /**
     * 回源解析：库中存在「启用且非空」的配置则用库值（敏感解密），否则回落 Environment。
     */
    private String resolve(String key) {
        DbRow row = readFromDb(key);
        if (row != null && row.value() != null && !row.value().isBlank()) {
            if (row.sensitive()) {
                try {
                    return crypto.decrypt(row.value());
                } catch (Exception e) {
                    log.error("敏感配置 {} 解密失败，回落 Environment", key, e);
                    return environment.getProperty(key);
                }
            }
            return row.value();
        }
        return environment.getProperty(key);
    }

    /**
     * 直读 sys_config；无 DataSource（如纯网关服务）或查询异常时返回 null（回落 Environment）。
     */
    private DbRow readFromDb(String key) {
        DataSource dataSource = dataSourceProvider.getIfAvailable();
        if (dataSource == null) {
            return null;
        }
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(SELECT_ENABLED_SQL)) {
            ps.setLong(1, PLATFORM_TENANT_ID);
            ps.setString(2, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new DbRow(rs.getString("config_value"), rs.getInt("is_sensitive") == 1);
                }
            }
        } catch (SQLException e) {
            log.warn("直读 sys_config 失败，回落 Environment：key={}, 原因={}", key, e.getMessage());
        }
        return null;
    }

    /** 库行：存储值（敏感时为密文）+ 是否敏感 */
    private record DbRow(String value, boolean sensitive) {
    }

    /** 缓存条目：解析后的值（允许为 null）+ 过期时间戳 */
    private record CacheEntry(String value, long expireAt) {
    }
}
