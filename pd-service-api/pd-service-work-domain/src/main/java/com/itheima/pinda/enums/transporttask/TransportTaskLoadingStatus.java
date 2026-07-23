package com.itheima.pinda.enums.transporttask;

import com.itheima.pinda.common.base.BaseStatusEnum;

/**
 * 运输任务满载状态
 *
 * @author itcast
 */

public enum TransportTaskLoadingStatus implements BaseStatusEnum<Integer, String> {
    /**
     * 空载
     */
    EMPTY(1, "空载"),
    /**
     * 半载
     */
    HALF(2, "半载"),
    /**
     * 满载
     */
    FULL(3, "满载");


    TransportTaskLoadingStatus(Integer code, String value) {

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
    public static TransportTaskLoadingStatus lookup(Integer code) {
        if (code == null) return null;
        for (TransportTaskLoadingStatus s : values()) {
            if (s.code.equals(code)) return s;
        }
        return null;
    }

}
