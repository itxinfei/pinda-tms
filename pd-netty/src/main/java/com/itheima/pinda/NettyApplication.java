package com.itheima.pinda;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * pd-netty 轨迹服务启动类
 *
 * <p>P0-4 北斗字段最小改造（D-13）启用 @EnableScheduling，
 * 供 GpsTraceConsumer 中 @Scheduled 心跳超时扫描任务使用（每 60s 一次）。
 * 符合 D-21：不引入 XXL-JOB，复用 Spring 原生 @Scheduled。</p>
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
public class NettyApplication {
    public static void main(String[] args) {
        SpringApplication.run(NettyApplication.class, args);
    }
}