package com.itheima.pinda;

import com.itheima.pinda.auth.server.EnableAuthServer;
import com.itheima.pinda.user.annotation.EnableLoginArgResolver;
import com.itheima.pinda.validator.config.EnableFormValidator;
import com.itheima.pinda.user.annotation.EnableLoginArgResolver;
import com.itheima.pinda.validator.config.EnableFormValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * https://mp.weixin.qq.com/s/zkxI5IQP0jFTjVYe5pTsXw
 * EnableAspectJAutoProxy(proxyTargetClass=true, exposeProxy=true) 配合 @EnableCaching
 * 才能解决在同一个类中通过 AopContext.currentProxy() 调用时，使缓存生效
 *
 * @author User
 */
@Slf4j
@SpringBootApplication
@EnableDiscoveryClient
@EnableAuthServer
@EnableFeignClients(value = {"com.itheima.pinda",})
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
@EnableLoginArgResolver
@EnableFormValidator
public class AuthorityApplication {

    public static void main(String[] args) throws UnknownHostException {
        ConfigurableApplicationContext context = SpringApplication.run(AuthorityApplication.class, args);
        ConfigurableEnvironment environment = context.getEnvironment();
        String appName = environment.getProperty("spring.application.name");
        String port = environment.getProperty("server.port");
        String hostAddress = InetAddress.getLocalHost().getHostAddress();

        //启动完成后在控制台提示项目启动成功，并且输出当前服务对应的swagger接口文档访问地址
        //http://localhost:8080/doc.html
        log.info("应用{}启动成功!swagger地址：http://{}:{}/doc.html", appName, hostAddress, port);
    }
}
