package com.xinfei.controller;

import com.xinfei.common.utils.Result;
import com.xinfei.service.SmsNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 短信发送内部接口 —— P0-7d
 *
 * <p>供 xf-auth 等服务经 Feign 调用（服务间 Same-Token 保护），复用
 * {@link SmsNotificationService} 的渠道路由（sms.active-channel）与降级链。
 * 阿里云真实通道需配置 SMS_ALIYUN_*；未配置时按渠道实现降级为日志，不阻断主流程。</p>
 */
@Slf4j
@RestController
@RequestMapping("sms")
@Tag(name = "短信发送(内部)")
public class SmsController {

    @Autowired
    private SmsNotificationService smsNotificationService;

    @Operation(summary = "发送短信（内部）")
    @PostMapping("/send")
    public Result send(@RequestParam("mobile") String mobile, @RequestParam("content") String content) {
        if (StringUtils.isBlank(mobile) || StringUtils.isBlank(content)) {
            return Result.error(400, "手机号与内容不能为空");
        }
        try {
            smsNotificationService.sendSms(mobile, content);
            return Result.ok();
        } catch (Exception e) {
            log.error("[短信] 发送异常: mobile={}", mobile, e);
            return Result.error(500, "短信发送失败");
        }
    }
}
