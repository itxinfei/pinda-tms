package com.itheima.pinda.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 定时任务失败重试参数配置
 *
 * <p>从配置中心（Nacos）读取，支持运行时调整，无需改代码：
 * <ul>
 *   <li>{@code schedule.retry.max-attempts}：最大尝试次数（含首次，默认 3）</li>
 *   <li>{@code schedule.retry.interval-ms}：重试间隔毫秒数（默认 2000）</li>
 * </ul></p>
 */
@Data
@Component
public class ScheduleRetryProperties {

    /**
     * 最大尝试次数（含首次）。例如 3 表示首次 + 2 次重试。
     */
    @Value("${schedule.retry.max-attempts:3}")
    private int maxAttempts;

    /**
     * 重试间隔（毫秒）
     */
    @Value("${schedule.retry.interval-ms:2000}")
    private long intervalMs;
}
