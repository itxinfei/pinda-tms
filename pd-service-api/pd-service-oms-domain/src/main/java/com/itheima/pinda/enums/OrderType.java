package com.itheima.pinda.enums;

import com.itheima.pinda.common.base.BaseStatusEnum;

/**
 * 订单类型枚举
 */
public enum OrderType implements BaseStatusEnum<Integer, String> {

    /**
     * 同城订单
     */
    INCITY(1, "同城订单"),

    /**
     * 城际订单
     */
    OUTCITY(2, "城际订单");

    OrderType(Integer code, String value) {
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
    public static OrderType lookup(Integer code) {
        if (code == null) return null;
        for (OrderType s : values()) {
            if (s.code.equals(code)) return s;
        }
        return null;
    }
}
