package com.itheima.pinda.common.config.dynamic;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 动态参数配置属性（CFG-01）。
 *
 * <ul>
 *   <li>{@code cryptoKey}：敏感配置 AES 主密钥。dev 默认值允许入代码（见 CLAUDE.md §5.2）；
 *       生产经环境变量 {@code PINDA_CONFIG_CRYPTO_KEY}（松散绑定 {@code pinda.config.crypto-key}）覆盖。</li>
 *   <li>{@code cacheTtlSeconds}：业务服务本地缓存 TTL，到期后直读 {@code sys_config}；
 *       后台修改后最长该秒数内全网生效，密钥调用失败可经 {@link DynamicConfigService#evict} 即时重读。</li>
 * </ul>
 */
@Data
@ConfigurationProperties(prefix = "pinda.config")
public class DynamicConfigProperties {

    /** 敏感配置 AES-256-GCM 主密钥（SHA-256 派生）；dev 内置默认，生产必须外置覆盖 */
    private String cryptoKey = "pinda-dev-config-crypto-key-20261011";

    /** 本地缓存 TTL（秒），默认 30s */
    private long cacheTtlSeconds = 30L;
}
