package com.itheima.pinda.enums.driverjob;

import com.itheima.pinda.common.base.BaseStatusEnum;

/**
 * 运输任务状态
 *
 * @author itcast
 */

public enum DriverJobStatus implements BaseStatusEnum<Integer, String> {
    /**
     * 待执行,对应 待提货
     */
    PENDING(1, "待执行"),
    /**
     * 进行中，对应 在途
     */
    PROCESSING(2, "进行中"),
    /**
     * 改派，对应已交付
     */
    CONFIRM(3, "改派"),
    /**
     * 已完成，对应 已交付
     */
    COMPLETED(4, "已完成"),
    /**
     * 已作废
     */
    CANCELLED(5, "已作废");


    DriverJobStatus(Integer code, String value) {

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
    public static DriverJobStatus lookup(Integer code) {
        if (code == null) return null;
        for (DriverJobStatus s : values()) {
            if (s.code.equals(code)) return s;
        }
        return null;
    }

}
