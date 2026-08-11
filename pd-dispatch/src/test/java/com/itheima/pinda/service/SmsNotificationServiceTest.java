package com.itheima.pinda.service;

import com.itheima.pinda.service.sms.SmsChannel;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * 短信通知服务单元测试
 *
 * <p>验证手机号脱敏规则与渠道选择逻辑（无外部依赖，通过反射注入模拟渠道）。</p>
 */
public class SmsNotificationServiceTest {

    private final SmsNotificationService service = new SmsNotificationService();

    @Test
    public void testMaskMobileNormal() {
        assertEquals("138****5678", service.maskMobile("13812345678"));
    }

    @Test
    public void testMaskMobileShort() {
        // 不足7位统一返回 ****
        assertEquals("****", service.maskMobile("123"));
    }

    @Test
    public void testMaskMobileNull() {
        assertEquals("****", service.maskMobile(null));
    }

    @Test
    public void testMaskMobileEmpty() {
        assertEquals("****", service.maskMobile(""));
    }

    @Test
    public void testMaskMobileExactSeven() {
        // 恰好7位：保留前3后4（重叠部分按规则截取，仍为掩码形式）
        String masked = service.maskMobile("1234567");
        assertTrue(masked != null && masked.length() > 0);
    }

    /**
     * 渠道选择：配置的渠道应被调用，且调用成功
     */
    @Test
    public void testSendSmsSelectsActiveChannel() throws Exception {
        AtomicBoolean called = new AtomicBoolean(false);
        SmsChannel mockChannel = new SmsChannel() {
            @Override
            public String channelCode() {
                return "mock-test";
            }

            @Override
            public boolean sendSms(String mobile, String content) {
                called.set(true);
                return true;
            }
        };
        List<SmsChannel> channels = new ArrayList<>();
        channels.add(mockChannel);

        setField("activeChannel", "mock-test");
        setField("smsChannels", channels);

        service.sendSms("13812345678", "测试短信");
        assertTrue("配置的渠道应被调用", called.get());
    }

    /**
     * 渠道选择：渠道发送失败时不应抛异常（日志兜底）
     */
    @Test
    public void testSendSmsFailureDoesNotThrow() throws Exception {
        SmsChannel failChannel = new SmsChannel() {
            @Override
            public String channelCode() {
                return "fail-test";
            }

            @Override
            public boolean sendSms(String mobile, String content) {
                return false;
            }
        };
        List<SmsChannel> channels = new ArrayList<>();
        channels.add(failChannel);

        setField("activeChannel", "fail-test");
        setField("smsChannels", channels);

        // 发送失败不应抛出异常
        service.sendSms("13812345678", "测试短信");
    }

    /**
     * 通过反射设置私有字段
     */
    private void setField(String name, Object value) throws Exception {
        Field field = SmsNotificationService.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(service, value);
    }
}
