package com.itheima.pinda.enums;

import com.itheima.pinda.common.base.BaseStatusEnum;

/**
 * 订单状态枚举
 *
 * <p>2026-10-10 重构v2：code 由旧五位 23000~23011 收敛为紧凑 TINYINT 1~12，
 * 与 oms_order.status 及 04_oms.sql 文件头映射一致。调用方一律使用枚举常量，
 * 不得硬编码状态数字。</p>
 */
public enum OrderStatus implements BaseStatusEnum<Integer, String> {

    /**
     * 待取件（旧 23000）
     */
    PENDING(1, "PENDING"),

    /**
     * 已取件（旧 23001）
     */
    PICKED_UP(2, "PICKED_UP"),

    /**
     * 网点自寄（旧 23002）
     */
    OUTLETS_SINCE_SENT(3, "OUTLETS_SINCE_SENT"),

    /**
     * 网点入库（旧 23003）
     */
    OUTLETS_WAREHOUSE(4, "OUTLETS_WAREHOUSE"),


    /**
     * 待装车（旧 23004）
     */
    FOR_LOADING(5, "FOR_LOADING"),


    /**
     * 运输中（旧 23005）
     */
    IN_TRANSIT(6, "IN_TRANSIT"),


    /**
     * 网点出库（旧 23006）
     */
    OUTLETS_EX_WAREHOUSE(7, "OUTLETS_EX_WAREHOUSE"),

    /**
     * 待派送（旧 23007）
     */
    TO_BE_DISPATCHED(8, "TO_BE_DISPATCHED"),

    /**
     * 派送中（旧 23008）
     */
    DISPATCHING(9, "DISPATCHING"),

    /**
     * 已签收（旧 23009）
     */
    RECEIVED(10, "RECEIVED"),

    /**
     * 拒收（旧 23010）
     */
    REJECTION(11, "REJECTION"),

    /**
     * 已取消（旧 23011）
     */
    CANCELLED(12, "CANCELLED");

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
