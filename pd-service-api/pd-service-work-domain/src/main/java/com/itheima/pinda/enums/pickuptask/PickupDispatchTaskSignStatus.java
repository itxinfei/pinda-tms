package com.itheima.pinda.enums.pickuptask;

import com.itheima.pinda.common.base.BaseStatusEnum;

/**
 * 取派件任务签收状态
 *
 * @author itcast
 */

public enum PickupDispatchTaskSignStatus implements BaseStatusEnum<Integer, String> {
    /**
     * 已签收
     */
    RECEIVED(1, "已签收"),
    /**
     * 拒收
     */
    REJECTION(2, "拒收");


    PickupDispatchTaskSignStatus(Integer code, String value) {

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
    public static PickupDispatchTaskSignStatus lookup(Integer code) {
        if (code == null) return null;
        for (PickupDispatchTaskSignStatus s : values()) {
            if (s.code.equals(code)) return s;
        }
        return null;
    }

}
