package com.itheima.pinda.common.config;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.itheima.pinda.common.CustomIdGenerator;
import com.itheima.pinda.common.utils.IdWorker;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * 自定义 ID 生成器自动装配：仅当类路径存在 MyBatis-Plus（IdentifierGenerator）时生效。
 *
 * <p>pd-common 以 optional 方式引入 mybatis-plus-core，pd-gateway/pd-auth 等无
 * MyBatis-Plus 的服务因 {@link ConditionalOnClass} 条件不成立而整体跳过装配。</p>
 */
@AutoConfiguration
@ConditionalOnClass(IdentifierGenerator.class)
public class CustomIdGeneratorAutoConfiguration {

    /**
     * 雪花算法核心。
     *
     * <p>保留历史固定 workerId/datacenterId=1：R3 为无行为变更重构，不改变 ID 位段。
     * 多实例部署如需防碰撞，可改用无参 {@code new IdWorker()} 自动按 MAC/PID 分配，
     * 属独立部署决策，不在本阶段夹带。</p>
     */
    @Bean
    @ConditionalOnMissingBean(IdWorker.class)
    public IdWorker idWorker() {
        return new IdWorker(1, 1);
    }

    /**
     * 桥接 MyBatis-Plus 主键生成
     */
    @Bean
    @ConditionalOnMissingBean(IdentifierGenerator.class)
    public CustomIdGenerator customIdGenerator(IdWorker idWorker) {
        return new CustomIdGenerator(idWorker);
    }
}
