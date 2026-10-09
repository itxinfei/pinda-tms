package com.itheima.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 品达 TMS 认证授权服务。
 * 登录 / RBAC / 组织 / 租户管理（JDK21 + Spring Boot 3.3 + Sa-Token）。
 */
@SpringBootApplication
public class AuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
