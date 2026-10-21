package com.xinfei.common.json;

import org.springframework.boot.jackson.JacksonComponent;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

import java.math.BigDecimal;

@JacksonComponent
public class BigDecimalDeSerializer extends ValueDeserializer<BigDecimal> {
    @Override
    public BigDecimal deserialize(JsonParser jsonParser,
                                  DeserializationContext deserializationContext) {
        //千分位分隔的数值从前端到后端是需要反序列化为BigDecimal。需要去掉"，"
        String text = jsonParser.getText();
        if (text == null || text.trim().isEmpty() || "null".equals(text.trim())) {
            return null;
        }
        return new BigDecimal(text.replaceAll(",", ""));
    }
}
