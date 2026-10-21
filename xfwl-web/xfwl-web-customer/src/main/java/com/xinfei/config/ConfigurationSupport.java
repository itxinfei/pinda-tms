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
import tools.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import tools.jackson.datatype.jsr310.ser.LocalDateSerializer;
import tools.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import tools.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import com.xinfei.common.converter.EnumDeserializer;
import com.xinfei.common.interceptor.TokenAuthInterceptor;
import com.xinfei.common.satoken.RequiredRoleInterceptor;
import com.xinfei.common.json.BigDecimalSerializer;
import com.xinfei.common.json.NullValueSerializerModifier;
import com.xinfei.common.utils.DatePatterns;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
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

import static tools.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

@Slf4j
@Configuration
public class ConfigurationSupport extends WebMvcConfigurationSupport {

    /**
     * 端级角色绑定（X-01 下沉落点）：本类 extends WebMvcConfigurationSupport 使 Boot MVC
     * 自动装配退避，xfwl-common 经 WebMvcConfigurer 注册的角色拦截器不被收集，须在此显式注册；
     * 属性为空（未配置 xinfei.auth.required-roles）时不注册。
     */
    @Value("${xinfei.auth.required-roles:}")
    private String requiredRoles;

    /**
     * LocalDateTime 反序列化兼容两种线上格式：
     * ① "yyyy-MM-dd HH:mm:ss"（全局 Jackson 输出）；② ISO "yyyy-MM-dd'T'HH:mm:ss"（xfwl-oms 默认输出）。
     * 只认①时，xfwl-oms 订单保存响应（含 estimatedArrivalTime）在 Feign 解码层失败走 fallback，下单被误报失败。
     * 注意：customizer Bean（Feign SpringDecoder 走容器 ObjectMapper）与下方 MVC 内部类
     * 是两条 ObjectMapper 构建路径，必须两处同时注册，写法对齐 xfwl-web-manager。
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
                        // 非"空格"格式，按 ISO-8601 兜底解析
                        return LocalDateTime.parse(value);
                    }
                }
            };

    /**
     * deserializerByType 解决string类型入参转为 LocalDateTime 格式问题
     * 本 Bean 定制容器自动配置的 ObjectMapper，Feign 的 SpringDecoder 解码 xfwl-oms 响应走的就是这条路径
     *
     * @return
     */
    @Bean
    public JsonMapperBuilderCustomizer jsonMapperBuilderCustomizer() {
        // Boot 4（Jackson 3）：Jackson2ObjectMapperBuilderCustomizer 已废弃，改用 JsonMapperBuilderCustomizer，
        // 通过 SimpleModule 承载同款 per-type 序列化/反序列化注册。
        SimpleModule module = new SimpleModule();
        module.addDeserializer((Class) Enum.class, (ValueDeserializer) EnumDeserializer.INSTANCE);
        module.addDeserializer(LocalDateTime.class, FLEXIBLE_LOCAL_DATE_TIME_DESERIALIZER);
        module.addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern(DatePatterns.DATE)));
        module.addDeserializer(LocalTime.class, new LocalTimeDeserializer(DateTimeFormatter.ofPattern(DatePatterns.TIME)));
        // 序列化侧对齐全局"空格"契约：此前只配反序列化，容器 mapper 对 LocalDateTime
        // 仍走 Boot 默认 ISO 输出，MQ 事件负载 eventTime 格式随机（5 处消费端补丁的根源）
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DatePatterns.DATE_TIME)));
        module.addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern(DatePatterns.DATE)));
        module.addSerializer(LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ofPattern(DatePatterns.TIME)));
        return builder -> builder.addModule(module);
    }

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
     * 注册 Token 鉴权拦截器（依赖网关透传 userid 头，直连端口时兜底拒绝）
     *
     * @param registry 拦截器注册器
     */
    @Override
    protected void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new TokenAuthInterceptor())
            .addPathPatterns("/**");
        // 端级角色绑定（X-01）：属性空则不注册，actuator 探针在静态方法内已排除
        RequiredRoleInterceptor.registerIfConfigured(registry, requiredRoles);
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
        // Jackson 3：MappingJackson2HttpMessageConverter.setObjectMapper 已移除，mapper 改由构造函数注入
        JacksonJsonHttpMessageConverter jacksonJsonHttpMessageConverter = new JacksonJsonHttpMessageConverter(buildJacksonObjectMapper());
        converters.add(jacksonJsonHttpMessageConverter);
        //追加默认转换器
        super.addDefaultHttpMessageConverters(converters);
    }

    private static JsonMapper buildJacksonObjectMapper() {
        SimpleModule simpleModule = new SimpleModule()
//                .addDeserializer(Enum.class, EnumDeserializer.INSTANCE)
//                .addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern(DatePatterns.DATE)))
//                .addDeserializer(LocalTime.class, new LocalTimeDeserializer(DateTimeFormatter.ofPattern(DatePatterns.TIME)))

                    .addSerializer(BigInteger.class, ToStringSerializer.instance)
                    .addSerializer(BigDecimal.class, new BigDecimalSerializer())
                    .addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DatePatterns.DATE_TIME)))
                    .addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern(DatePatterns.DATE)))
                    .addSerializer(LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ofPattern(DatePatterns.TIME)))
                    .addSerializer(Date.class, new JavaUtilDateSerializer(false, new SimpleDateFormat(DatePatterns.DATE_TIME)))
                    // 与 customizer Bean 共用同一双格式反序列化器（MVC 入站路径）
                    .addDeserializer(LocalDateTime.class, FLEXIBLE_LOCAL_DATE_TIME_DESERIALIZER)
                    .setSerializerModifier(new NullValueSerializerModifier());

        // Jackson 3：ObjectMapper 不可变，所有配置经 JsonMapper.builder() 完成
        return JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .addModule(simpleModule)
                .build();
    }
}
