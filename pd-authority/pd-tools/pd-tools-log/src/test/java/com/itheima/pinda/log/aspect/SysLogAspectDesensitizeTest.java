package com.itheima.pinda.log.aspect;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 操作日志入参脱敏测试
 *
 * <p>SysLogAspect 会把方法全部入参序列化成 JSON 存进 pd_opt_log.params，
 * 而 /user/password（@SysLog("修改密码")）的 DTO 里带 oldPassword/password/confirmPassword，
 * 不打码就等于把口令原文长期留在日志表里。</p>
 */
public class SysLogAspectDesensitizeTest {

    @Test
    public void testPasswordFieldsMasked() {
        String in = "[{\"oldPassword\":\"Abc@12345\",\"password\":\"NewPass1\",\"confirmPassword\":\"NewPass1\"},\"ctx\"]";
        String out = SysLogAspect.desensitize(in);

        assertFalse("口令原文不能留在日志里: " + out, out.contains("Abc@12345"));
        assertFalse(out.contains("NewPass1"));
        assertTrue(out.contains("\"password\":\"***\""));
        assertTrue(out.contains("\"oldPassword\":\"***\""));
        assertTrue("无关入参要保持原样: " + out, out.contains("\"ctx\""));
    }

    @Test
    public void testMobileAndIdCardMasked() {
        String out = SysLogAspect.desensitize("[{\"mobile\":\"13812345678\",\"idCard\":\"110101199001011234\"}]");

        assertFalse(out.contains("13812345678"));
        assertFalse(out.contains("110101199001011234"));
    }

    @Test
    public void testNumericValueMasked() {
        // JSON 里手机号/证件号常常是数字字面量，没有引号也要能命中
        String out = SysLogAspect.desensitize("[{\"phone\":13800000000}]");

        assertFalse(out.contains("13800000000"));
        assertTrue(out.contains("\"phone\":\"***\""));
    }

    @Test
    public void testNonSensitiveFieldsUntouched() {
        String in = "[{\"ids\":[1,2],\"username\":\"admin\",\"status\":0}]";

        assertEquals("非敏感字段不应被改写", in, SysLogAspect.desensitize(in));
    }

    @Test
    public void testWordInsideValueDoesNotTriggerMask() {
        // 只有键名算敏感，值里出现 password 这个词不该把整段业务文本打码
        String in = "[{\"remark\":\"客户说忘记过一次password，已提醒\"}]";

        assertEquals(in, SysLogAspect.desensitize(in));
    }

    @Test
    public void testNullAndEmptyPassThrough() {
        assertEquals(null, SysLogAspect.desensitize(null));
        assertEquals("", SysLogAspect.desensitize(""));
    }
}
