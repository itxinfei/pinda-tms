package com.itheima.pinda.enums;

import com.itheima.pinda.common.base.BaseStatusEnum;

/**
 * 身份证号验证状态
 *
 * @author itcast
 */

public enum MemberIdCardVerifyStatus implements BaseStatusEnum<Integer, String> {
    /**
     * 未验证
     */
    NONE(0, "未验证"),
    /**
     * 验证通过
     */
    SUCCESS(1, "验证通过"),
    /**
     * 验证失败
     */
    FAIL(2, "验证失败");


    MemberIdCardVerifyStatus(Integer code, String value) {

        this.code = code;
        this.value = value;
    }

    /**
     * 类型编码
     */
    private final Integer code;

    /**
     * 类型值
     */
    private final String value;


    @Override
    public Integer getCode() {
        return code;
    }

    @Override
    public String getValue() {
        return value;
    }

    /**
     * 根据code获取枚举项
     *
     * @param code 值
     * @return 值
     */
    public static MemberIdCardVerifyStatus lookup(Integer code) {
        if (code == null) return null;
        for (MemberIdCardVerifyStatus s : values()) {
            if (s.code.equals(code)) return s;
        }
        return null;
    }

}
