package com.xinfei.common.json;

import java.util.List;
import java.util.Map;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.ser.ValueSerializerModifier;

/**
 * null 值序列化修饰器（Boot4 / Jackson 3 迁移）。
 * <p>
 * 背景：Jackson 2 时代通过 {@code ObjectMapper.setNullValueSerializer(...)} 全局定制 null 输出
 * （youlai 框架惯例：String 空值转 ""、List 转 []、Map 转 {}）。Jackson 3 已移除该 API，
 * 且原 {@code BeanSerializerModifier} 更名为 {@link ValueSerializerModifier}，
 * 故改用模块级 {@code SimpleModule.setSerializerModifier} 在序列化器构建期为每个属性指派 null 序列化器。
 * <p>
 * 行为与原实现一致（按属性的声明类型判定，比旧的运行时反射更稳）：
 * String -> ""、List -> []、Map -> {}、其余类型 -> ""。
 * 注意：与旧实现相同，属性上标注 @JsonInclude(NON_NULL) 等抑制 null 时本修饰器不生效。
 * <p>
 * 用法：{@code simpleModule.setSerializerModifier(new NullValueSerializerModifier());}
 */
public class NullValueSerializerModifier extends ValueSerializerModifier {

    @Override
    public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
            BeanDescription.Supplier beanDescRef, List<BeanPropertyWriter> beanProperties) {
        for (BeanPropertyWriter writer : beanProperties) {
            writer.assignNullSerializer(nullSerializerFor(writer.getType()));
        }
        return beanProperties;
    }

    /**
     * 按属性声明类型返回对应的 null 输出序列化器。
     */
    private static ValueSerializer<Object> nullSerializerFor(JavaType type) {
        Class<?> raw = type.getRawClass();
        if (raw == String.class) {
            return new ValueSerializer<Object>() {
                @Override
                public void serialize(Object value, JsonGenerator gen, SerializationContext ctxt) {
                    //字符串型空值 ""
                    gen.writeString("");
                }
            };
        }
        if (raw == List.class) {
            return new ValueSerializer<Object>() {
                @Override
                public void serialize(Object value, JsonGenerator gen, SerializationContext ctxt) {
                    //列表型空值 []
                    gen.writeStartArray();
                    gen.writeEndArray();
                }
            };
        }
        if (raw == Map.class) {
            return new ValueSerializer<Object>() {
                @Override
                public void serialize(Object value, JsonGenerator gen, SerializationContext ctxt) {
                    //map型空值 {}
                    gen.writeStartObject();
                    gen.writeEndObject();
                }
            };
        }
        return new ValueSerializer<Object>() {
            @Override
            public void serialize(Object value, JsonGenerator gen, SerializationContext ctxt) {
                //默认返回 ""
                gen.writeString("");
            }
        };
    }
}
