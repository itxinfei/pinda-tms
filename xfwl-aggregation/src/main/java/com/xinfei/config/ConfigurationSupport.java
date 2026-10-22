package com.xinfei.config;

import tools.jackson.databind.ser.jdk.JavaUtilDateSerializer;
import tools.jackson.databind.ser.std.ToStringSerializer;
import tools.jackson.databind.ext.javatime.deser.LocalDateDeserializer;
import tools.jackson.databind.ext.javatime.deser.LocalDateTimeDeserializer;
import tools.jackson.databind.ext.javatime.deser.LocalTimeDeserializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateSerializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateTimeSerializer;
import tools.jackson.databind.ext.javatime.ser.LocalTimeSerializer;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.module.SimpleModule;
import com.xinfei.common.converter.EnumDeserializer;
import com.xinfei.common.interceptor.SameTokenAuthInterceptor;
import com.xinfei.common.json.BigDecimalSerializer;
import com.xinfei.common.utils.DatePatterns;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import static tools.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

/**
 * MVC / Jackson / 接口文档统一配置（数据聚合服务）。
 * Boot4 迁移：不再继承 WebMvcConfigurationSupport（继承会关闭 Spring MVC 自动配置），
 * Jackson 定制改用 Jackson2ObjectMapperBuilderCustomizer 由自动配置生效；
 * springfox 替换为 springdoc；EnumDeserializer、日期格式常量复用 xfwl-common。
 * 序列化口径与旧版一致（本服务仅 BigInteger/BigDecimal/时间/Date 转字符串）。
 */
@Slf4j
@Configuration
public class ConfigurationSupport implements WebMvcConfigurer {
    /**
     * Jackson 序列化 / 反序列化定制（枚举、时间格式，BigDecimal/BigInteger 转字符串）
     */
    @Bean
    public JsonMapperBuilderCustomizer jsonMapperBuilderCustomizer() {
        log.info("初始化jackson配置( 枚举类型、时间格式 )");
        // Boot 4（Jackson 3）：Jackson2ObjectMapperBuilderCustomizer 已废弃，改用 JsonMapperBuilderCustomizer，
        // 通过 SimpleModule 承载同款 per-type 序列化/反序列化注册。
        SimpleModule module = new SimpleModule();
        // 反序列化
        module.addDeserializer((Class) Enum.class, (ValueDeserializer) EnumDeserializer.INSTANCE);
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DatePatterns.DATE_TIME)));
        module.addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern(DatePatterns.DATE)));
        module.addDeserializer(LocalTime.class, new LocalTimeDeserializer(DateTimeFormatter.ofPattern(DatePatterns.TIME)));
        // 序列化（保持本服务原有项）
        module.addSerializer(BigInteger.class, ToStringSerializer.instance);
        module.addSerializer(BigDecimal.class, new BigDecimalSerializer());
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DatePatterns.DATE_TIME)));
        module.addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern(DatePatterns.DATE)));
        module.addSerializer(LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ofPattern(DatePatterns.TIME)));
        module.addSerializer(Date.class, new JavaUtilDateSerializer(false, new SimpleDateFormat(DatePatterns.DATE_TIME)));
        return builder -> {
            builder.disable(FAIL_ON_UNKNOWN_PROPERTIES);
            builder.addModule(module);
        };
    }

    /**
     * springdoc 接口文档基本信息
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("心飞物流管理数据聚合服务--Swagger文档")
                        .version("1.0"));
    }

    /**
     * Same-Token 入站鉴权（T-SEC-04：堵住绕过网关/BFF 直连服务端口的漏洞）。
     * 凭证由网关 ForwardSameTokenFilter 或调用方 Feign 出站拦截器注入；放行 /actuator/**
     * （compose HTTP 探活无凭证——《12》§8 部署红线，恒放行）、favicon/error/swagger 文档。
     *
     * @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SameTokenAuthInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns("/actuator/**", "/favicon.ico", "/error",
                        "/swagger-ui/**", "/v3/api-docs/**", "/webjars/**");
    }
}
