package com.itheima.pinda.config;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

/**
 * 异步线程池配置
 *
 * 用于调度任务的异步执行，提高并发处理能力
 * 核心参数:
 * - 核心线程数: 5（最小并发任务数）
 * - 最大线程数: 20（峰值并发能力）
 * - 队列容量: 100（缓冲待执行任务）
 * - 线程存活时间: 60秒（空闲线程超时回收）
 *
 * @author Claude Code
 * @since 2026-07-23
 */
@Configuration
@EnableAsync
public class AsyncTaskConfig implements AsyncConfigurer {

    /**
     * 调度任务异步线程池
     */
    @Bean(name = "scheduleTaskExecutor")
    public ThreadPoolTaskExecutor scheduleTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数
        executor.setCorePoolSize(5);
        // 最大线程数
        executor.setMaxPoolSize(20);
        // 队列容量
        executor.setQueueCapacity(100);
        // 线程名前缀
        executor.setThreadNamePrefix("schedule-task-");
        // 空闲线程存活时间（秒）
        executor.setKeepAliveSeconds(60);
        // 拒绝策略：由调用线程执行（避免任务丢失）
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        // 等待任务完成后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // 等待超时时间（秒）
        executor.setAwaitTerminationSeconds(30);
        // 初始化线程池
        executor.initialize();
        return executor;
    }

    /**
     * 通用异步线程池（用于事件处理等）
     */
    @Bean(name = "commonAsyncExecutor")
    public ThreadPoolTaskExecutor commonAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("common-async-");
        executor.setKeepAliveSeconds(60);
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return scheduleTaskExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> {
            // 记录异步方法未捕获的异常
            org.slf4j.LoggerFactory.getLogger(AsyncTaskConfig.class)
                .error("异步方法执行异常: method={}, params={}", method.getName(), params, ex);
        };
    }
}
