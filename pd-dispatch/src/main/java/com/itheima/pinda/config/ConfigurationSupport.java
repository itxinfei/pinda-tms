package com.itheima.pinda.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.DateSerializer;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import com.itheima.pinda.common.converter.EnumDeserializer;
import com.itheima.pinda.common.json.BigDecimalSerializer;
import com.itheima.pinda.common.utils.DatePatterns;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

@Slf4j
@Configuration
public class ConfigurationSupport extends WebMvcConfigurationSupport {
    /**
     * deserializerByType 解决string类型入参转为 LocalDateTime 格式问题
     *
     * @return
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
        // MQ 消费（OrderEventMQListener）用容器 ObjectMapper 反序列化事件；
        // 生产端（各服务 EventPublisher）eventTime 输出可能是 ISO 带 T+纳秒
        // （如 "2026-10-12T18:51:02.509947852"），只认空格格式会解析失败进死信队列，
        // 故与 pd-web-manager/pd-web-customer 同款：兼容"空格"与 ISO 两种格式。
        return builder -> builder
                .deserializerByType(Enum.class, EnumDeserializer.INSTANCE)
                .deserializerByType(LocalDateTime.class, FLEXIBLE_LOCAL_DATE_TIME_DESERIALIZER)
                .deserializerByType(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern(DatePatterns.DATE)))
                .deserializerByType(LocalTime.class, new LocalTimeDeserializer(DateTimeFormatter.ofPattern(DatePatterns.TIME)));

    }

    /**
     * LocalDateTime 反序列化兼容两种线上格式：
     * ① "yyyy-MM-dd HH:mm:ss"（全局 Jackson 契约输出）；② ISO "yyyy-MM-dd'T'HH:mm:ss[.纳秒]"（JavaTimeModule 默认输出）。
     */
    private static final JsonDeserializer<LocalDateTime> FLEXIBLE_LOCAL_DATE_TIME_DESERIALIZER =
            new JsonDeserializer<LocalDateTime>() {
                @Override
                public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                    String value = p.getValueAsString();
                    if (value == null || value.isBlank()) {
                        return null;
                    }
                    value = value.trim();
                    try {
                        return LocalDateTime.parse(value, DateTimeFormatter.ofPattern(DatePatterns.DATE_TIME));
                    } catch (DateTimeParseException ignored) {
                        // 非"空格"格式，按 ISO-8601 兜底解析（含纳秒）
                        return LocalDateTime.parse(value);
                    }
                }
            };

    /**
     * 防止@EnableMvc把默认的静态资源路径覆盖了，手动设置的方式
     *
     * @param registry
     */
    @Override
    protected void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 解决静态资源无法访问
        registry.addResourceHandler("/**").addResourceLocations("classpath:/static/");
        // 解决swagger无法访问
        registry.addResourceHandler("/swagger-ui.html").addResourceLocations("classpath:/META-INF/resources/");
        // 解决swagger的js文件无法访问
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");

    }

    /**
     * 解决序列化null问题
     * 默认转成 ''
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        log.info("初始化jackson配置( null -> ''、枚举类型、时间格式 )");
        //在json转换之前先进行string转换
        converters.add(new StringHttpMessageConverter());
        //添加json转换
        MappingJackson2HttpMessageConverter jackson2HttpMessageConverter = new MappingJackson2HttpMessageConverter();
        jackson2HttpMessageConverter.setObjectMapper(new JacksonObjectMapper());
        converters.add(jackson2HttpMessageConverter);
        //追加默认转换器
        super.addDefaultHttpMessageConverters(converters);
    }

    class JacksonObjectMapper extends ObjectMapper {
        public JacksonObjectMapper() {
            super();
            //收到未知属性时不报异常
            this.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);

            //反序列化时，属性不存在的兼容处理
            this.getDeserializationConfig().withoutFeatures(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

            SimpleModule simpleModule = new SimpleModule()
//                .addDeserializer(Enum.class, EnumDeserializer.INSTANCE)
//                .addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DatePatterns.DATE_TIME)))
//                .addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern(DatePatterns.DATE)))
//                .addDeserializer(LocalTime.class, new LocalTimeDeserializer(DateTimeFormatter.ofPattern(DatePatterns.TIME)))

                    .addSerializer(BigInteger.class, ToStringSerializer.instance)
                    //雪花ID精度铁律：对外 Long 统一序列化为 String，避免 JS Number 精度丢失
                    .addSerializer(Long.class, ToStringSerializer.instance)
                    .addSerializer(Long.TYPE, ToStringSerializer.instance)
                    .addSerializer(BigDecimal.class, new BigDecimalSerializer())
                    .addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DatePatterns.DATE_TIME)))
                    .addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern(DatePatterns.DATE)))
                    .addSerializer(LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ofPattern(DatePatterns.TIME)))
                    .addSerializer(Date.class, new DateSerializer(false, new SimpleDateFormat(DatePatterns.DATE_TIME)));

            this.registerModule(simpleModule);
            //处理空指针时设置的值
            this.getSerializerProvider().setNullValueSerializer(new JsonSerializer<Object>() {
                @Override
                public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                    String fieldName = gen.getOutputContext().getCurrentName();
                    try {
                        //反射获取字段类型
                        Field field = gen.currentValue().getClass().getDeclaredField(fieldName);
                        if (Objects.equals(field.getType(), String.class)) {
                            //字符串型空值""
                            gen.writeString("");
                            return;
                        } else if (Objects.equals(field.getType(), List.class)) {
                            //列表型空值返回[]
                            gen.writeStartArray();
                            gen.writeEndArray();
                            return;
                        } else if (Objects.equals(field.getType(), Map.class)) {
                            //map型空值返回{}
                            gen.writeStartObject();
                            gen.writeEndObject();
                            return;
                        }
                    } catch (NoSuchFieldException e) {
                    }
                    //默认返回""
                    gen.writeString("");
                }
            });
        }
    }
}
