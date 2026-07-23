package com.itheima.pinda.enums;

import com.itheima.pinda.common.base.BaseStatusEnum;

/**
 * 订单类型枚举
 */
public enum OrderPaymentMethod implements BaseStatusEnum<Integer, String> {

    /**
     * 预结
     */
    PRE_PAY(1, "预结"),

    /**
     * 到付
     */
    END_PAY(2, "到付");

    OrderPaymentMethod(Integer code, String value) {
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
    public static OrderPaymentMethod lookup(Integer code) {
        if (code == null) return null;
        for (OrderPaymentMethod s : values()) {
            if (s.code.equals(code)) return s;
        }
        return null;
    }
}
