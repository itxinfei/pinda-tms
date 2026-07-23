package com.itheima.pinda.enums.transportorder;

import com.itheima.pinda.common.base.BaseStatusEnum;

/**
 * 运单-调度状态
 */

public enum TransportOrderSchedulingStatus implements BaseStatusEnum<Integer, String> {
    /**
     * 待调度
     */
    TO_BE_SCHEDULED(1, "待调度"),

    /**
     * 未匹配到线路
     */
    NO_MATCH_TRANSPORTLINE(2, "未匹配到线路"),

    /**
     * 已调度
     */
    SCHEDULED(3, "已调度");


    TransportOrderSchedulingStatus(Integer code, String value) {

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
    public static TransportOrderSchedulingStatus lookup(Integer code) {
        if (code == null) return null;
        for (TransportOrderSchedulingStatus s : values()) {
            if (s.code.equals(code)) return s;
        }
        return null;
    }

}
