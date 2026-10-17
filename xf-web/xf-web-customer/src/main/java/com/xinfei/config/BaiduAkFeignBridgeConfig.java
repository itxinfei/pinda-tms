package com.xinfei.config;

import com.xinfei.common.config.dynamic.BaiduAkBridge;
import com.xinfei.common.config.dynamic.DynamicConfigService;
import com.xinfei.feign.ConfigFeign;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Supplier;

/**
 * 百度 AK 动态桥 · BFF 版（CFG-01）：BFF 无数据源，无法直读 sys_config，
 * 覆盖 xf-common 默认桥为「ConfigFeign 读 xf-auth」链路，实现「AK 存库、后台可改、不重启生效」。
 * 客户端（下单地址解析/取件围栏判定）与 manager/driver 同构，覆盖后 sys_config 改 AK 即时生效。
 *
 * <p>读取优先级：ConfigFeign（30s 本地 TTL，熔断/未装配时跳过）
 * → 本地 DynamicConfigService（无库时退化为 Environment）
 * → JVM {@code -Dbaidu.map.ak}。</p>
 *
 * <p>Bean 名与 xf-common 自动装配一致（baiduAkBridge），自动装配按类型
 * {@code @ConditionalOnMissingBean} 让位，本配置优先生效。</p>
 */
@Slf4j
@Configuration
public class BaiduAkFeignBridgeConfig {

    private static final long TTL_MS = 30_000L;

    @Bean("baiduAkBridge")
    public BaiduAkBridge baiduAkBridge(ObjectProvider<ConfigFeign> configFeignProvider,
                                       ObjectProvider<DynamicConfigService> dynamicConfigProvider) {
        // Feign 结果 30s TTL 缓存：避免每次地理编码调用都打一次 xf-auth
        final long[] fetchedAt = {0L};
        final String[] cached = {null};
        Supplier<String> feignFirst = () -> {
            long now = System.currentTimeMillis();
            if (now - fetchedAt[0] > TTL_MS) {
                cached[0] = null;
                ConfigFeign feign = configFeignProvider.getIfAvailable();
                if (feign != null) {
                    try {
                        cached[0] = feign.valueByKey(BaiduAkBridge.CONFIG_KEY_BAIDU_AK);
                    } catch (Exception e) {
                        log.warn("Feign 读取百度AK失败，回落本地/启动参数: {}", e.getMessage());
                    }
                }
                fetchedAt[0] = now;
            }
            if (cached[0] != null && !cached[0].isBlank()) {
                return cached[0];
            }
            DynamicConfigService local = dynamicConfigProvider.getIfAvailable();
            if (local != null) {
                String v = local.get(BaiduAkBridge.CONFIG_KEY_BAIDU_AK, "");
                if (v != null && !v.isBlank()) {
                    return v;
                }
            }
            return System.getProperty(BaiduAkBridge.CONFIG_KEY_BAIDU_AK, "");
        };
        return new BaiduAkBridge(feignFirst);
    }
}
