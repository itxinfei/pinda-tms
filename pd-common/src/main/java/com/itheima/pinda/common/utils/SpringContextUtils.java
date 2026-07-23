package com.itheima.pinda.common.utils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Spring Context 工具类
 */
@Component
public class SpringContextUtils implements ApplicationContextAware {
    private static volatile ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
            throws BeansException {
        SpringContextUtils.applicationContext = applicationContext;
    }

    public static Object getBean(String name) {
        ApplicationContext ctx = applicationContext;
        if (ctx == null) {
            throw new IllegalStateException("Spring容器尚未初始化，无法获取Bean: " + name);
        }
        return ctx.getBean(name);
    }

    public static <T> T getBean(Class<T> cls) {
        ApplicationContext ctx = applicationContext;
        if (ctx == null) {
            throw new IllegalStateException("Spring容器尚未初始化，无法获取Bean: " + cls.getName());
        }
        return ctx.getBean(cls);
    }

    public static <T> T getBean(String name, Class<T> requiredType) {
        ApplicationContext ctx = applicationContext;
        if (ctx == null) {
            throw new IllegalStateException("Spring容器尚未初始化，无法获取Bean: " + name);
        }
        return ctx.getBean(name, requiredType);
    }

    public static boolean containsBean(String name) {
        ApplicationContext ctx = applicationContext;
        if (ctx == null) {
            throw new IllegalStateException("Spring容器尚未初始化");
        }
        return ctx.containsBean(name);
    }

    public static boolean isSingleton(String name) {
        ApplicationContext ctx = applicationContext;
        if (ctx == null) {
            throw new IllegalStateException("Spring容器尚未初始化");
        }
        return ctx.isSingleton(name);
    }

    public static Class<? extends Object> getType(String name) {
        ApplicationContext ctx = applicationContext;
        if (ctx == null) {
            throw new IllegalStateException("Spring容器尚未初始化");
        }
        return ctx.getType(name);
    }

}