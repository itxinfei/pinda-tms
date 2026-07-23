package com.itheima.pinda.enums.pickuptask;

import com.itheima.pinda.common.base.BaseStatusEnum;

/**
 * 取派件任务分配状态
 *
 * @author itcast
 */

public enum PickupDispatchTaskAssignedStatus implements BaseStatusEnum<Integer, String> {
    /**
     * 待分配
     */
    TO_BE_DISTRIBUTED(1, "待分配"),
    /**
     * 已分配
     */
    DISTRIBUTED(2, "已分配"),
    /**
     * 待人工分配
     */
    MANUAL_DISTRIBUTED(3, "待人工分配");


    PickupDispatchTaskAssignedStatus(Integer code, String value) {

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
    public static PickupDispatchTaskAssignedStatus lookup(Integer code) {
        if (code == null) return null;
        for (PickupDispatchTaskAssignedStatus s : values()) {
            if (s.code.equals(code)) return s;
        }
        return null;
    }

}
