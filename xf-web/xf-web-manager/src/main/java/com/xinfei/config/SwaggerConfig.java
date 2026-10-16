package com.xinfei.config;

import com.xinfei.common.interceptor.TokenAuthInterceptor;
import com.xinfei.interceptor.OperLogInterceptor;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * springdoc-openapi 接口文档配置。
 * 文档地址：http://localhost:8186/swagger-ui/index.html
 */
@Configuration
public class SwaggerConfig implements WebMvcConfigurer {

    private final OperLogInterceptor operLogInterceptor;

    // 修改点：构造器参数加 @Lazy 破启动期 Bean 循环依赖（swaggerConfig→operLogInterceptor→LogFeign→环，
    // P0-9 部署联调暴露的启动失败；@Lazy 生成代理延迟到首次使用时解析 LogFeign，拦截器注册行为不变）
    public SwaggerConfig(@Lazy OperLogInterceptor operLogInterceptor) {
        this.operLogInterceptor = operLogInterceptor;
    }

    @Bean
    public OpenAPI customOpenApi() {
        return new OpenAPI().info(new Info()
                .title("心飞物流管理后台--接口文档")
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
        // 写请求审计（SYS-06 跨服务埋点）：POST/PUT/DELETE 经 Feign 上报 xf-auth，失败不影响业务
        registry.addInterceptor(operLogInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/actuator/**", "/internal/**", "/error");
    }
}
