package com.xinfei.common.json;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.jackson.JacksonComponent;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.SerializationContext;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Objects;

@Slf4j
@JacksonComponent
public class BigDecimalSerializer extends ValueSerializer<BigDecimal> {
    private String format = "##0.00";

    @Override
    public void serialize(BigDecimal bigDecimal, JsonGenerator jsonGenerator, SerializationContext serializerProvider) {
        jsonGenerator.writeString(new DecimalFormat(format).format(bigDecimal));
    }

    @Override
    public ValueSerializer<?> createContextual(SerializationContext serializerProvider, BeanProperty beanProperty) {
        if (beanProperty != null) {
            if (Objects.equals(beanProperty.getType().getRawClass(), BigDecimal.class)) {
                BigDecimalFormat bigDecimalFormat = beanProperty.getAnnotation((BigDecimalFormat.class));
                if (bigDecimalFormat == null) {
                    bigDecimalFormat = beanProperty.getContextAnnotation(BigDecimalFormat.class);
                }
                BigDecimalSerializer bigDecimalSerializer = new BigDecimalSerializer();
                if (bigDecimalFormat != null) {
                    bigDecimalSerializer.format = bigDecimalFormat.value();
                }
                return bigDecimalSerializer;
            }
            return serializerProvider.findValueSerializer(beanProperty.getType());
        }
        // 修改点：Map 值/根级序列化无 BeanProperty 上下文，必须返回可正常输出的默认序列化器；
        // 原实现返回 findNullValueSerializer 会把【非空】BigDecimal 写成 null
        // （P0-6d 联调暴露：profit/summary/ranking 顶层 income/cost/profit 全 null，而 marginRate 有值）。
        return new BigDecimalSerializer();
    }
}
