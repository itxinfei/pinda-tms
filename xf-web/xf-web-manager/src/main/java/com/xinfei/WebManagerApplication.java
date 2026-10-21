package com.xinfei;

import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.JsonParser;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;
import tools.jackson.datatype.jsr310.JavaTimeModule;
import tools.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import tools.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import tools.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import tools.jackson.datatype.jsr310.ser.LocalDateSerializer;
import tools.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import tools.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import com.xinfei.common.converter.EnumDeserializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignFormatterRegistrar;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
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
     * ① "yyyy-MM-dd HH:mm:ss"（xf-work/xf-base 等全局 Jackson 输出）；
     * ② ISO "yyyy-MM-dd'T'HH:mm:ss"（xf-oms 的 LocalDateTime 字段经 JavaTimeModule 默认序列化输出）。
     * 只认①时，xf-oms 订单分页响应（含 estimatedArrivalTime）解码失败被 fallback 吞成空页。
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
        //   ① "yyyy-MM-dd HH:mm:ss"（xf-work/xf-base 等经全局 Jackson 序列化输出）；
        //   ② ISO "yyyy-MM-dd'T'HH:mm:ss"（xf-oms 的 LocalDateTime 字段经 JavaTimeModule 默认序列化输出，
        //      如 OrderDTO.estimatedArrivalTime）。只认①时，xf-oms 分页响应解码失败被 fallback 吞成空页。
        // Boot 4（Jackson 3）：Jackson2ObjectMapperBuilderCustomizer 已废弃，改用 JsonMapperBuilderCustomizer，
        // 通过 SimpleModule 承载同款 per-type 序列化/反序列化注册。
        DateTimeFormatter spaceDateTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        SimpleModule module = new SimpleModule()
                .addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(spaceDateTime))
                .addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .addSerializer(LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ofPattern("HH:mm:ss")))
                .addDeserializer(LocalDateTime.class, FLEXIBLE_LOCAL_DATE_TIME_DESERIALIZER)
                .addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .addDeserializer(LocalTime.class, new LocalTimeDeserializer(DateTimeFormatter.ofPattern("HH:mm:ss")));
        return builder -> builder.addModule(module);
    }
    /**
     * 解决序列化和反序列化时，参数转换问题
     * addSerializer：序列化 （Controller 返回 给前端的json）
     * Long -> string
     * BigInteger -> string
     * BigDecimal -> string
     * date -> string
     * <p>
     * addDeserializer: 反序列化 （前端调用接口时，传递到后台的json）
     * 枚举类型: {"code": "xxx"} -> Enum
     *
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean
    public ObjectMapper jacksonObjectMapper() {
        DateTimeFormatter spaceDateTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // 单引号 / 转义 / 控制字符：Jackson 3 中 JsonParser.Feature 已改名为 tools.jackson.core.json.JsonReadFeature
        JsonFactory jsonFactory = JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
                .enable(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER)
                .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
                .build();

        // Java 8 时间默认模块：Jackson 3 的 JavaTimeModule 为 final 且不再暴露 addSerializer/addDeserializer，
        // 自定义格式化注册改由下方 SimpleModule 承载；本模块仍提供 Instant/ZonedDateTime 等默认注册。
        JavaTimeModule javaTimeModule = new JavaTimeModule();

        SimpleModule simpleModule = new SimpleModule()
                .addSerializer(BigInteger.class, ToStringSerializer.instance)
                .addSerializer(BigDecimal.class, ToStringSerializer.instance)
                .addDeserializer((Class) Enum.class, (ValueDeserializer) EnumDeserializer.INSTANCE)
                // 时间序列化契约（与上方 customizer 保持一致）
                .addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(spaceDateTime))
                .addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .addSerializer(LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ofPattern("HH:mm:ss")))
                // LocalDateTime 反序列化同 customizer：兼容"空格"与 ISO 两种线上格式
                .addDeserializer(LocalDateTime.class, FLEXIBLE_LOCAL_DATE_TIME_DESERIALIZER)
                .addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .addDeserializer(LocalTime.class, new LocalTimeDeserializer(DateTimeFormatter.ofPattern("HH:mm:ss")));

        // Jackson 3：ObjectMapper 不可变，所有配置经 JsonMapper.builder() 一次性完成
        // 注意：Builder 方法名为 defaultLocale/defaultTimeZone/defaultDateFormat；
        // SerializationFeature.WRITE_DATES_AS_TIMESTAMPS 在 Jackson 3 已移除（时间类型默认按字符串输出）。
        return JsonMapper.builder(jsonFactory)
                .defaultLocale(Locale.CHINA)
                .defaultTimeZone(TimeZone.getTimeZone("Asia/Shanghai"))
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                //忽略未知字段（兼容前端多余字段）
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                //旧 Date 类型统一格式
                .defaultDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"))
                .addModule(javaTimeModule)
                .addModule(simpleModule)
                .build();
    }
}
