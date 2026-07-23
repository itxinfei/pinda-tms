package com.itheima.pinda.enums;

import com.itheima.pinda.common.base.BaseStatusEnum;

/**
 * 订单类型枚举
 */
public enum OrderPickupType implements BaseStatusEnum<Integer, String> {

    /**
     * 网点自寄
     */
    NO_PICKUP(1, "网点自寄"),

    /**
     * 上门取件
     */
    PICKUP(2, "上门取件");

    OrderPickupType(Integer code, String value) {
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
    public static OrderPickupType lookup(Integer code) {
        if (code == null) return null;
        for (OrderPickupType s : values()) {
            if (s.code.equals(code)) return s;
        }
        return null;
    }
}
