package com.itheima.pinda.enums;

/**
 * 业务对象类型枚举（business_type 数字码唯一真源）
 *
 * <p>上报协议用别名 {@code truck/courier}，iot 表 {@code business_type} 用数字码
 * （1车辆 2快递员）。本枚举负责别名与数字码转换，替代各处裸数字。</p>
 */
public enum BusinessType {

    /** 车辆 */
    TRUCK(1, "truck", "车辆"),

    /** 快递员 */
    COURIER(2, "courier", "快递员");

    private final int code;
    private final String alias;
    private final String description;

    BusinessType(int code, String alias, String description) {
        this.code = code;
        this.alias = alias;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getAlias() {
        return alias;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 上报别名（truck/courier）→ 数字码，空白或不匹配返回 null
     */
    public static Integer codeOfAlias(String alias) {
        if (alias == null || alias.trim().isEmpty()) {
            return null;
        }
        for (BusinessType item : values()) {
            if (item.alias.equalsIgnoreCase(alias.trim())) {
                return item.code;
            }
        }
        return null;
    }
}
