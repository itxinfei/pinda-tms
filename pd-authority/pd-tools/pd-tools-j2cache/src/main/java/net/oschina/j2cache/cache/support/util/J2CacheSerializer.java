package net.oschina.j2cache.cache.support.util;

import lombok.extern.slf4j.Slf4j;
import net.oschina.j2cache.util.SerializationUtils;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.io.IOException;

/**
 * J2Cache Redis序列化器
 */
@Slf4j
public class J2CacheSerializer implements RedisSerializer<Object> {

    @Override
    public byte[] serialize(Object t) throws SerializationException {
        try {
            return SerializationUtils.serialize(t);
        } catch (IOException e) {
            log.error("J2Cache序列化失败: {}", t != null ? t.getClass().getName() : "null", e);
            throw new SerializationException("序列化失败", e);
        }
    }

    @Override
    public Object deserialize(byte[] bytes) throws SerializationException {
        try {
            return SerializationUtils.deserialize(bytes);
        } catch (IOException e) {
            log.error("J2Cache反序列化失败", e);
            throw new SerializationException("反序列化失败", e);
        }
    }
}
