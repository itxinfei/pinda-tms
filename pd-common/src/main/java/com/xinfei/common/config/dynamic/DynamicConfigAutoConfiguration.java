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
 * <p>所有引入 pd-common 的服务均装配：{@link ConfigCrypto} 供管理端加密/脱敏；
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
}
