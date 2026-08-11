package com.itheima.pinda.service;

import com.itheima.pinda.service.sms.SmsChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 短信/通知服务
 *
 * <p>订单确认、揽收完成、交付(签收/拒收)等业务事件统一通过本组件向客户发送通知。
 * 支持多渠道（可扩展）：
 * <ol>
 *   <li><b>通用 HTTP 网关</b>：{@code sms.channel.http.url}，适配自建/第三方聚合网关；</li>
 *   <li><b>阿里云短信</b>：{@code sms.channel.aliyun.*}；</li>
 *   <li><b>腾讯云短信</b>：{@code sms.channel.tencent.*}；</li>
 *   <li><b>华为云短信</b>：{@code sms.channel.huawei.*}；</li>
 *   <li><b>通用设备接口</b>：{@code sms.channel.device.url}，适配短信猫/短信池等设备；</li>
 *   <li><b>日志兜底</b>：未配置任何渠道时仅记录日志，不影响业务主流程。</li>
 * </ol>
 * 通过 {@code sms.channel} 指定启用渠道（http/aliyun/tencent/huawei/device），
 * 缺省按优先级尝试通用 HTTP → 日志兜底。</p>
 */
@Slf4j
@Component
public class SmsNotificationService {

    /**
     * 启用的短信渠道编码（http/aliyun/tencent/huawei/device）
     */
    @Value("${sms.channel:http}")
    private String activeChannel;

    /**
     * 所有短信渠道实现（Spring 注入）
     */
    @Autowired(required = false)
    private List<SmsChannel> smsChannels;

    /**
     * 发送短信通知
     *
     * @param mobile  手机号
     * @param content 短信内容
     */
    public void sendSms(String mobile, String content) {
        // 1. 日志记录（手机号脱敏）
        log.info("[短信通知] 收件人: {}, 内容: {}", maskMobile(mobile), content);

        if (smsChannels == null || smsChannels.isEmpty()) {
            log.info("[短信通知] 未配置短信渠道，仅记录日志");
            return;
        }

        // 2. 按配置选择渠道发送
        boolean sent = false;
        for (SmsChannel channel : smsChannels) {
            if (activeChannel == null || activeChannel.trim().isEmpty()
                    || activeChannel.equalsIgnoreCase(channel.channelCode())) {
                if (channel.sendSms(mobile, content)) {
                    sent = true;
                    break;
                }
            }
        }
        if (!sent) {
            log.warn("[短信通知] 渠道[{}]发送未成功，消息仅记录日志: mobile={}", activeChannel, maskMobile(mobile));
        }
    }

    /**
     * 手机号脱敏：保留前3后4，中间用*号
     * （包内可见，便于单元测试验证脱敏规则）
     *
     * @param mobile 手机号
     * @return 脱敏后手机号
     */
    String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 7) {
            return "****";
        }
        return mobile.substring(0, 3) + "****" + mobile.substring(mobile.length() - 4);
    }
}
