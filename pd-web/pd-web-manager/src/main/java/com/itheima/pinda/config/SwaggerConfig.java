package com.itheima.pinda.config;

import com.itheima.pinda.common.interceptor.TokenAuthInterceptor;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * springdoc-openapi 接口文档配置。
 * 文档地址：http://localhost:8186/swagger-ui/index.html
 */
@Configuration
public class SwaggerConfig implements WebMvcConfigurer {

    @Bean
    public OpenAPI customOpenApi() {
        return new OpenAPI().info(new Info()
                .title("品达物流管理后台--接口文档")
                .version("1.0"));
    }

    /**
     * 注册 Token 鉴权拦截器（依赖网关透传 userid 头，直连端口时兜底拒绝）
     *
     * @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new TokenAuthInterceptor())
                .addPathPatterns("/**");
    }
}
