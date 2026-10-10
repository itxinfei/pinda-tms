package com.itheima.pinda.config;

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
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

/**
 * MVC / Jackson / 接口文档统一配置（数据聚合服务）。
 * Boot3 迁移：不再继承 WebMvcConfigurationSupport（继承会关闭 Spring MVC 自动配置），
 * Jackson 定制改用 Jackson2ObjectMapperBuilderCustomizer 由自动配置生效；
 * springfox 替换为 springdoc；EnumDeserializer、日期格式常量复用 pd-common。
 * 序列化口径与旧版一致（本服务仅 BigInteger/BigDecimal/时间/Date 转字符串）。
 */
@Slf4j
@Configuration
public class ConfigurationSupport {
    /**
     * Jackson 序列化 / 反序列化定制（枚举、时间格式，BigDecimal/BigInteger 转字符串）
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
        log.info("初始化jackson配置( 枚举类型、时间格式 )");
        return builder -> builder
                .featuresToDisable(FAIL_ON_UNKNOWN_PROPERTIES)
                // 反序列化
                .deserializerByType(Enum.class, EnumDeserializer.INSTANCE)
                .deserializerByType(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DatePatterns.DATE_TIME)))
                .deserializerByType(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern(DatePatterns.DATE)))
                .deserializerByType(LocalTime.class, new LocalTimeDeserializer(DateTimeFormatter.ofPattern(DatePatterns.TIME)))
                // 序列化（保持本服务原有项）
                .serializerByType(BigInteger.class, ToStringSerializer.instance)
                .serializerByType(BigDecimal.class, new BigDecimalSerializer())
                .serializerByType(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DatePatterns.DATE_TIME)))
                .serializerByType(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern(DatePatterns.DATE)))
                .serializerByType(LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ofPattern(DatePatterns.TIME)))
                .serializerByType(Date.class, new DateSerializer(false, new SimpleDateFormat(DatePatterns.DATE_TIME)));
    }

    /**
     * springdoc 接口文档基本信息
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("品达物流管理数据聚合服务--Swagger文档")
                        .version("1.0"));
    }
}
