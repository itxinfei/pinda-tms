package com.itheima.pinda.service;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * 短信通知服务单元测试
 *
 * <p>验证手机号脱敏规则（无外部依赖，纯逻辑测试）。</p>
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
}
