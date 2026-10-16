package com.xinfei.feign;

import com.xinfei.common.utils.Result;
import com.xinfei.feign.fallback.DispatchSmsFeignFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 短信发送内部 Feign（P0-7d），对接 xf-dispatch /sms/send。
 *
 * <p>auth 登录/绑定手机验证码经此走真实短信通道（sms.active-channel 路由，
 * 未配置真实渠道时 dispatch 侧降级为日志，流程不阻断）。</p>
 */
@FeignClient(name = "xf-dispatch", path = "/sms", fallback = DispatchSmsFeignFallback.class)
public interface DispatchSmsFeign {

    @PostMapping("/send")
    Result send(@RequestParam("mobile") String mobile, @RequestParam("content") String content);
}
