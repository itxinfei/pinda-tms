package com.xinfei.common.config.dynamic;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

/**
 * 动态参数配置自动装配（CFG-01）。
 *
 * <p>所有引入 xf-common 的服务均装配：{@link ConfigCrypto} 供管理端加密/脱敏；
 * {@link DynamicConfigService} 在有 {@link DataSource} 时直读同库 sys_config，
 * 无 DataSource 时退化为纯 Spring Environment 读取（如网关），无副作用。</p>
 */
@AutoConfiguration
@EnableConfigurationProperties(DynamicConfigProperties.class)
public class DynamicConfigAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ConfigCrypto configCrypto(DynamicConfigProperties properties) {
        return new ConfigCrypto(properties.getCryptoKey());
    }

    @Bean
    @ConditionalOnMissingBean
    public DynamicConfigService dynamicConfigService(ObjectProvider<DataSource> dataSourceProvider,
                                                     Environment environment,
                                                     ConfigCrypto configCrypto,
                                                     DynamicConfigProperties properties) {
        return new DynamicConfigService(
                dataSourceProvider, environment, configCrypto, properties.getCacheTtlSeconds());
    }

    /**
     * 把动态配置链路接入地理服务客户端 BaiduGeoClient（百度 AK 存库热读取，批3 收敛后唯一出口）。
     * 默认链路 = 参数库 sys_config → Environment(-D/环境变量)；DynamicConfigService 内部含 30s TTL 缓存；
     * 百度 status!=0 时经第二参 evict 参数库 AK 本地缓存。
     * BFF 等无库服务可注册同名 Bean 覆盖为「Feign 读 xf-auth」链路。
     */
    @Bean
    @ConditionalOnMissingBean
    public BaiduAkBridge baiduAkBridge(ObjectProvider<DynamicConfigService> dynamicConfigProvider) {
        return new BaiduAkBridge(
                () -> {
                    DynamicConfigService service = dynamicConfigProvider.getIfAvailable();
                    return service != null
                            ? service.get(BaiduAkBridge.CONFIG_KEY_BAIDU_AK, "")
                            : System.getProperty(BaiduAkBridge.CONFIG_KEY_BAIDU_AK, "");
                },
                key -> {
                    DynamicConfigService service = dynamicConfigProvider.getIfAvailable();
                    if (service != null) {
                        service.evict(key);
                    }
                });
    }
}
