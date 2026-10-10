package com.itheima.pinda.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger相关配置类（springdoc-openapi）
 */
@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        // 接口文档基本信息
        return new OpenAPI()
                .info(new Info()
                        .title("品达物流订单模块--Swagger文档")
                        .version("1.0"));
    }
}
