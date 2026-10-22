package com.xinfei.config;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.jdk.JavaUtilDateSerializer;
import tools.jackson.databind.ser.std.ToStringSerializer;
import tools.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import tools.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import tools.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import tools.jackson.datatype.jsr310.ser.LocalDateSerializer;
import tools.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import tools.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import com.xinfei.common.converter.EnumDeserializer;
import com.xinfei.common.interceptor.SameTokenAuthInterceptor;
import com.xinfei.common.json.BigDecimalSerializer;
import com.xinfei.common.json.NullValueSerializerModifier;
import com.xinfei.common.utils.DatePatterns;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

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

import static tools.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

@Slf4j
@Configuration
@lombok.RequiredArgsConstructor
public class ConfigurationSupport implements WebMvcConfigurer {

    private final com.xinfei.interceptor.OperLogInterceptor operLogInterceptor;

    /**
     * deserializerByType 解决string类型入参转为 LocalDateTime 格式问题
     *
     * @return
     */
    @Bean
    public JsonMapperBuilderCustomizer jsonMapperBuilderCustomizer() {
        // MQ 消费（OrderEventMQListener）用容器 ObjectMapper 反序列化事件；
        // 生产端（各服务 EventPublisher）eventTime 输出可能是 ISO 带 T+纳秒
        // （如 "2026-10-12T18:51:02.509947852"），只认空格格式会解析失败进死信队列，
        // 故与 xfwl-web-manager/xfwl-web-customer 同款：兼容"空格"与 ISO 两种格式。
        // Boot 4（Jackson 3）：Jackson2ObjectMapperBuilderCustomizer 已废弃，改用 JsonMapperBuilderCustomizer，
        // 通过 SimpleModule 承载同款 per-type 反序列化注册。
        SimpleModule module = new SimpleModule();
        module.addDeserializer((Class) Enum.class, (ValueDeserializer) EnumDeserializer.INSTANCE);
        module.addDeserializer(LocalDateTime.class, FLEXIBLE_LOCAL_DATE_TIME_DESERIALIZER);
        module.addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern(DatePatterns.DATE)));
        module.addDeserializer(LocalTime.class, new LocalTimeDeserializer(DateTimeFormatter.ofPattern(DatePatterns.TIME)));
        return builder -> builder.addModule(module);
    }

    /**
     * LocalDateTime 反序列化兼容两种线上格式：
     * ① "yyyy-MM-dd HH:mm:ss"（全局 Jackson 契约输出）；② ISO "yyyy-MM-dd'T'HH:mm:ss[.纳秒]"（JavaTimeModule 默认输出）。
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
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 解决静态资源无法访问
        registry.addResourceHandler("/**").addResourceLocations("classpath:/static/");
        // 解决swagger无法访问
        registry.addResourceHandler("/swagger-ui.html").addResourceLocations("classpath:/META-INF/resources/");
        // 解决swagger的js文件无法访问
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");

    }

    /**
     * Same-Token 入站鉴权（T-SEC-04：堵住绕过网关/BFF 直连服务端口的漏洞）。
     * 本类继承 WebMvcConfigurationSupport 会关闭 MVC 自动配置，独立 WebMvcConfigurer bean
     * 不会被收集，故须直接 override 本方法注册。放行 /actuator/**（compose HTTP 探活
     * 无凭证——《12》§8 部署红线，恒放行）、favicon/error/swagger 文档。
     *
     * @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SameTokenAuthInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns("/actuator/**", "/favicon.ico", "/error",
                        "/swagger-ui/**", "/v3/api-docs/**", "/webjars/**");
        // 写请求审计（SYS-06 跨服务埋点）：POST/PUT/DELETE 落 OPER 日志，失败不影响业务
        registry.addInterceptor(operLogInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/actuator/**", "/internal/**", "/error");
    }

    /**
     * 解决序列化null问题
     * 默认转成 ''
     */
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        log.info("初始化jackson配置( null -> ''、枚举类型、时间格式 )");
        // Boot4 迁移：不再继承 WebMvcConfigurationSupport（其 configureMessageConverters /
        // addDefaultHttpMessageConverters 已 @Deprecated(forRemoval=true)，Spring Framework 7 将移除）。
        // 改用 WebMvcConfigurer.extendMessageConverters 在 Boot 默认转换器列表上做增量：
        // 移除 Boot 自动注入的默认 Jackson converter，替换为保留 null->''、Long->String 等
        // 全局 JSON 契约的定制版，并保留其余默认 converter（表单/字节/资源等）。
        converters.removeIf(c -> c instanceof JacksonJsonHttpMessageConverter);
        // 在 json 转换之前先进行 string 转换
        converters.add(0, new StringHttpMessageConverter());
        // 添加自定义 json 转换（保留现有序列化契约）
        JacksonJsonHttpMessageConverter jacksonJsonHttpMessageConverter = new JacksonJsonHttpMessageConverter(buildJacksonObjectMapper());
        converters.add(1, jacksonJsonHttpMessageConverter);
    }

    private static JsonMapper buildJacksonObjectMapper() {
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
                    .addSerializer(Date.class, new JavaUtilDateSerializer(false, new SimpleDateFormat(DatePatterns.DATE_TIME)))
                    .setSerializerModifier(new NullValueSerializerModifier());

        // Jackson 3：ObjectMapper 不可变，所有配置经 JsonMapper.builder() 完成
        return JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .addModule(simpleModule)
                .build();
    }
}
