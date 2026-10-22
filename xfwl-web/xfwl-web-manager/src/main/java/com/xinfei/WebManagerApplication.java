package com.xinfei;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;
import tools.jackson.databind.ext.javatime.deser.LocalDateDeserializer;
import tools.jackson.databind.ext.javatime.deser.LocalDateTimeDeserializer;
import tools.jackson.databind.ext.javatime.deser.LocalTimeDeserializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateSerializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateTimeSerializer;
import tools.jackson.databind.ext.javatime.ser.LocalTimeSerializer;
import com.xinfei.common.converter.EnumDeserializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignFormatterRegistrar;
import org.springframework.context.annotation.Bean;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.TimeZone;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class WebManagerApplication {
    public static void main(String[] args) {
        SpringApplication.run(WebManagerApplication.class, args);
    }
    /**
     * serializerByType 解决json中返回的 LocalDateTime 格式问题
     * deserializerByType 解决string类型入参转为 LocalDateTime 格式问题
     *
     * @return
     */
    /**
     * LocalDateTime 反序列化：兼容两种线上格式——
     * ① "yyyy-MM-dd HH:mm:ss"（xfwl-work/xfwl-base 等全局 Jackson 输出）；
     * ② ISO "yyyy-MM-dd'T'HH:mm:ss"（xfwl-oms 的 LocalDateTime 字段经 Jackson3 java.time 内建序列化输出）。
     * 只认①时，xfwl-oms 订单分页响应（含 estimatedArrivalTime）解码失败被 fallback 吞成空页。
     */
    private static final ValueDeserializer<LocalDateTime> FLEXIBLE_LOCAL_DATE_TIME_DESERIALIZER =
            new ValueDeserializer<LocalDateTime>() {
                @Override
                public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) {
                    String value = p.getValueAsString();
                    if (value == null || value.isBlank()) {
                        return null;
                    }
                    value = value.trim();
                    try {
                        return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    } catch (DateTimeParseException ignored) {
                        // 非"空格"格式，按 ISO-8601 兜底解析
                        return LocalDateTime.parse(value);
                    }
                }
            };

    @Bean
    public JsonMapperBuilderCustomizer jsonMapperBuilderCustomizer() {
        // 日期时间序列化契约（沿用旧 DateUtils 常量：date-time / date / time）
        // LocalDateTime 反序列化必须同时兼容两种线上格式：
        //   ① "yyyy-MM-dd HH:mm:ss"（xfwl-work/xfwl-base 等经全局 Jackson 序列化输出）；
        //   ② ISO "yyyy-MM-dd'T'HH:mm:ss"（xfwl-oms 的 LocalDateTime 字段经 Jackson3 java.time 内建序列化输出，
        //      如 OrderDTO.estimatedArrivalTime）。只认①时，xfwl-oms 分页响应解码失败被 fallback 吞成空页。
        // Boot 4（Jackson 3）：Jackson2ObjectMapperBuilderCustomizer 已废弃，改用 JsonMapperBuilderCustomizer，
        // 通过 SimpleModule 承载同款 per-type 序列化/反序列化注册。
        DateTimeFormatter spaceDateTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        SimpleModule module = new SimpleModule()
                .addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(spaceDateTime))
                .addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .addSerializer(LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ofPattern("HH:mm:ss")))
                .addDeserializer(LocalDateTime.class, FLEXIBLE_LOCAL_DATE_TIME_DESERIALIZER)
                .addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .addDeserializer(LocalTime.class, new LocalTimeDeserializer(DateTimeFormatter.ofPattern("HH:mm:ss")))
                // 数值精度契约：大数统一按字符串输出，避免前端 Number 精度丢失
                .addSerializer(BigInteger.class, ToStringSerializer.instance)
                .addSerializer(BigDecimal.class, ToStringSerializer.instance)
                // 枚举：{"code": "xxx"} -> Enum
                .addDeserializer((Class) Enum.class, (ValueDeserializer) EnumDeserializer.INSTANCE);
        return builder -> builder
                .defaultLocale(Locale.CHINA)
                .defaultTimeZone(TimeZone.getTimeZone("Asia/Shanghai"))
                // 旧 Date 类型统一格式
                .defaultDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"))
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                // 忽略未知字段（兼容前端多余字段）
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                // java.time 序列化支持已内建于 Jackson 3 databind（tools.jackson.databind.ext.javatime.*），无需再注册 JavaTimeModule
                .addModule(module);
    }
}
