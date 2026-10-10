package com.itheima.pinda.common.converter;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import lombok.extern.slf4j.Slf4j;

/**
 * enum 反序列化工具（下沉自旧 pd-tools-common）。
 * 从 JSON 中读取 code 字段值，再反射调用枚举类型的静态 get(String) 方法得到枚举实例。
 * Boot3 迁移：仅将 hutool 的 ReflectUtil.getField 替换为等价的纯 JDK 反射实现，行为不变。
 */
@Slf4j
public class EnumDeserializer extends StdDeserializer<Enum<?>> {
    public final static EnumDeserializer INSTANCE = new EnumDeserializer();
    private final static String ALL_ENUM_STRING_CONVERT_METHOD = "get";
    private final static String ALL_ENUM_KEY_FIELD = "code";

    public EnumDeserializer() {
        super(Enum.class);
    }

    @Override
    public Enum<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        JsonToken token = p.getCurrentToken();
        String value = null;
        while (!token.isStructEnd()) {
            if (ALL_ENUM_KEY_FIELD.equals(p.getText())) {
                p.nextToken();
                value = p.getValueAsString();
            } else {
                p.nextToken();
            }
            token = p.getCurrentToken();
        }
        if (value == null || "".equals(value)) {
            return null;
        }

        Object obj = p.getCurrentValue();
        if (obj == null) {
            return null;
        }
        Field field = findField(obj.getClass(), p.getCurrentName());
        //找不到字段
        if (field == null) {
            return null;
        }
        Class<?> fieldType = field.getType();
        try {
            Method method = fieldType.getMethod(ALL_ENUM_STRING_CONVERT_METHOD, String.class);
            return (Enum<?>) method.invoke(null, value);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | InvocationTargetException e) {
            log.warn("解析枚举失败", e);
            return null;
        }
    }

    /**
     * 在类及其父类、接口层次中查找指定名称的字段，找不到返回 null。
     * 等价于旧实现使用的 hutool ReflectUtil.getField。
     */
    private static Field findField(Class<?> clazz, String name) {
        Class<?> searchType = clazz;
        while (searchType != null && searchType != Object.class) {
            try {
                return searchType.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                searchType = searchType.getSuperclass();
            }
        }
        for (Class<?> iface : clazz.getInterfaces()) {
            try {
                return iface.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                // 当前接口无此字段，继续检查下一个
            }
        }
        return null;
    }
}
