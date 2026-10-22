package com.xinfei.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 心飞物流TMS 认证授权服务。
 * 登录 / RBAC / 组织 / 租户管理（JDK 25 + Spring Boot 4.1.1 + Sa-Token）。
 */
@SpringBootApplication
@EnableFeignClients(basePackages = "com.xinfei.feign")
public class AuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
