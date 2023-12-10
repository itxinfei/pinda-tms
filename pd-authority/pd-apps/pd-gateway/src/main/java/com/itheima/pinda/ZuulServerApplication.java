package com.itheima.pinda;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.itheima.pinda.auth.client.EnableAuthClient;

import com.itheima.pinda.auth.client.EnableAuthClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

/**
 * 网关代理
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients({"com.itheima.pinda"})
@EnableZuulProxy//开启网关代理
@EnableAuthClient//开启授权客户端，开启后就可以使用pd-tools-jwt提供的工具类进行jwt token解析了
@Slf4j
public class ZuulServerApplication {
    public static void main(String[] args) throws UnknownHostException {
        ConfigurableApplicationContext application = SpringApplication.run(ZuulServerApplication.class, args);
        Environment env = application.getEnvironment();
        log.info("\n----------------------------------------------------------\n\t" + "应用 '{}' 运行成功! 访问连接:\n\t" + "Swagger文档: \t\thttp://{}:{}{}{}/doc.html\n\t" + "----------------------------------------------------------", env.getProperty("spring.application.name"), InetAddress.getLocalHost().getHostAddress(), env.getProperty("server.port"), env.getProperty("server.servlet.context-path", ""), env.getProperty("spring.mvc.servlet.path", ""));
    }
}
