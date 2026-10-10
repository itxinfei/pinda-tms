package com.itheima.pinda;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
// work 域 Feign（work 自身）+ oms OrderFeign 均位于 com.itheima.pinda.feign 包；
// 显式启用后 TaskTransportServiceImpl 的 Order/TransportOrder 联动走真实 LB，不再注入 fallback 空转
@EnableFeignClients(basePackages = "com.itheima.pinda.feign")
public class WorkApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkApplication.class, args);
    }

}
