package com.itheima.pinda.enums;

import com.itheima.pinda.common.base.BaseStatusEnum;

/**
 * 订单状态枚举
 */
public enum OrderStatus implements BaseStatusEnum<Integer, String> {

    /**
     * 待取件
     */
    PENDING(23000, "PENDING"),

    /**
     * 已取件
     */
    PICKED_UP(23001, "PICKED_UP"),

    /**
     * 网点自寄
     */
    OUTLETS_SINCE_SENT(23002, "OUTLETS_SINCE_SENT"),

    /**
     * 网点入库
     */
    OUTLETS_WAREHOUSE(23003, "OUTLETS_WAREHOUSE"),


    /**
     * 待装车
     */
    FOR_LOADING(23004, "FOR_LOADING"),


    /**
     * 运输中
     */
    IN_TRANSIT(23005, "IN_TRANSIT"),


    /**
     * 网点出库
     */
    OUTLETS_EX_WAREHOUSE(23006, "OUTLETS_EX_WAREHOUSE"),

    /**
     * 待派送
     */
    TO_BE_DISPATCHED(23007, "TO_BE_DISPATCHED"),

    /**
     * 派送中
     */
    DISPATCHING(23008, "DISPATCHING"),

    /**
     * 已签收
     */
    RECEIVED(23009, "RECEIVED"),

    /**
     * 拒收
     */
    REJECTION(23010, "REJECTION"),

    /**
     * 已取消
     */
    CANCELLED(23011, "CANCELLED");

    OrderStatus(Integer code, String value) {

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
    public static OrderStatus lookup(Integer code) {
        if (code == null) return null;
        for (OrderStatus s : values()) {
            if (s.code.equals(code)) return s;
        }
        return null;
    }
}
