package com.itheima.pinda;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * @author User
 */
@SpringBootApplication
@EnableDiscoveryClient
public class AggregationApplication {
    public static void main(String[] args) {
        SpringApplication.run(AggregationApplication.class, args);
    }
}
