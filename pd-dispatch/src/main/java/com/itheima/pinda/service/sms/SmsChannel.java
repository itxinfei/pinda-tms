package com.itheima.pinda.service.sms;

/**
 * 短信渠道抽象接口
 *
 * <p>统一各短信平台（阿里云/腾讯云/华为云等）与通用设备（HTTP网关/短信猫等）的接入契约，
 * 供 {@code SmsNotificationService} 按配置选择渠道发送短信。</p>
 */
public interface SmsChannel {

    /**
     * 渠道编码（与配置 sms.channel 对应）
     *
     * @return 渠道编码
     */
    String channelCode();

    /**
     * 发送短信
     *
     * @param mobile  手机号
     * @param content 短信内容
     * @return 是否发送成功
     */
    boolean sendSms(String mobile, String content);
}
