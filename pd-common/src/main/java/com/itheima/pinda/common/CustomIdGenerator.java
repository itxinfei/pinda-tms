package com.itheima.pinda.common;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.itheima.pinda.common.utils.IdWorker;

/**
 * 自定义主键生成器：把 MyBatis-Plus 主键生成桥接到雪花算法 IdWorker。
 *
 * <p>不直接标注 {@code @Component}，由 {@code CustomIdGeneratorAutoConfiguration}
 * 在类路径存在 MyBatis-Plus 时按条件注册，避免 pd-gateway/pd-auth 等不使用
 * MyBatis-Plus 的服务因加载本类而找不到 IdentifierGenerator 接口。</p>
 */
public class CustomIdGenerator implements IdentifierGenerator {

    private final IdWorker idWorker;

    public CustomIdGenerator(IdWorker idWorker) {
        this.idWorker = idWorker;
    }

    /**
     * 生成唯一 ID（委托雪花算法）
     */
    @Override
    public Long nextId(Object entity) {
        return idWorker.nextId();
    }
}
