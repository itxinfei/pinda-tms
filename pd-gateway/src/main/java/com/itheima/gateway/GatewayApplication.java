package com.itheima.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 品达 TMS 系统唯一网关（Spring Cloud Gateway）。
 * 统一入口 8760：Sa-Token 鉴权 + 路由转发 + Same-Token 内部防绕过。
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
