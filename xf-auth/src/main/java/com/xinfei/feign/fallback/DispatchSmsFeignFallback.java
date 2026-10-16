package com.xinfei.feign.fallback;

import com.xinfei.common.utils.Result;
import com.xinfei.feign.DispatchSmsFeign;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 短信发送 Feign 熔断降级（调用方据此降级为固定码/日志，不阻断登录流程）
 */
@Slf4j
@Component
public class DispatchSmsFeignFallback implements DispatchSmsFeign {

    @Override
    public Result send(String mobile, String content) {
        log.warn("远程调用 xf-dispatch 失败: send(mobile={})", mobile);
        return Result.error("服务降级，短信发送失败");
    }
}
